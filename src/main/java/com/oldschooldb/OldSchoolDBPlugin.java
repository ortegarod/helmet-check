package com.oldschooldb;

import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.events.AccountHashChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.slayer.SlayerConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "OldSchoolDB Connector",
	description = "Sync your account to your OldSchoolDB profile at oldschooldb.com"
)
public class OldSchoolDBPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OldSchoolDBConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	private AuthService authService;
	private boolean isAuthenticated = false;
	private boolean showAuthMessageOnLogin = false;
	private boolean authenticationAttempted = false;
	// Set on the LOGGED_IN edge; the login greeting/auth chat is posted on the first
	// game tick after getLocalPlayer() is ready (see onGameTick) rather than here.
	// Posting chat on the LOGGED_IN edge itself runs before the client is fully loaded,
	// which makes other plugins' onChatMessage handlers (e.g. LootTracker) NPE.
	private boolean pendingGreeting = false;
	private Long currentAccountHash = null;
	// True once we've handled a login for the current session. LOGGED_IN fires again
	// on every map-region load, so this lets us greet + full-resync only on the real
	// login and ignore the region-load edges. Reset when we return to the login screen.
	private boolean loggedInThisSession = false;
	// Debounce state for skills/quests. StatChanged fires on every XP drop, so we
	// coalesce those bursts into throttled syncs instead of one send (and, for
	// quests, one full re-scan) per tick — the previous behaviour caused client lag.
	private boolean skillsDirty = false;
	private boolean questsDirty = false;
	private int lastSkillSyncTick = 0;
	private int lastQuestSyncTick = 0;
	private static final int SKILL_SYNC_INTERVAL_TICKS = 5;   // ~3s
	private static final int QUEST_SYNC_INTERVAL_TICKS = 50;  // ~30s
	// Cached display name of the current character. getLocalPlayer() is null for the
	// first frames after login (RuneLite gotcha), so the login-edge container syncs
	// can't read the name. We cache it the moment it becomes available and reuse it.
	private String cachedPlayerName = null;

	private static final String PROD_SERVER_URL = "https://api.oldschooldb.com";
	private static final String DEV_SERVER_URL = "http://localhost:3001";

	// Developers run RuneLite with -Doldschooldb.dev=true to point at the local
	// backend. Regular users never see this and always hit prod.
	private static boolean isDevMode()
	{
		return Boolean.getBoolean("oldschooldb.dev");
	}

	private static String resolveServerUrl()
	{
		return isDevMode() ? DEV_SERVER_URL : PROD_SERVER_URL;
	}

	// In dev mode the local backend needs its own plugin key, separate from the
	// prod key the user keeps in the config field. Pull the dev key from a VM
	// property (-Doldschooldb.devToken=...) or the OLDSCHOOLDB_DEV_TOKEN env var so
	// switching environments is just flipping the dev flag — no field swapping, and
	// nothing dev-related ever ships in the user-facing config. Falls back to the
	// config field if no dev key is supplied.
	private String resolveApiToken()
	{
		if (isDevMode()) {
			String devToken = System.getProperty("oldschooldb.devToken");
			if (devToken == null || devToken.isEmpty()) {
				devToken = System.getenv("OLDSCHOOLDB_DEV_TOKEN");
			}
			if (devToken != null && !devToken.isEmpty()) {
				return devToken;
			}
		}
		return config.apiToken();
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("OldSchoolDB Connector started!");
		authService = new AuthService(resolveServerUrl(), okHttpClient, gson);

		// Test connection to server
		authService.testConnection().thenAccept(error -> {
			if (error == null) {
				log.info("Connected to OldSchoolDB server");
				attemptAuthentication();
			} else {
				log.warn("Connection failed: {}", error);
				clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"OldSchoolDB: " + error, null));
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("OldSchoolDB Connector stopped!");
		isAuthenticated = false;
		authenticationAttempted = false; // Reset for next startup
	}

	private void attemptAuthentication() {
		// Prevent duplicate authentication attempts
		if (authenticationAttempted) {
			return;
		}
		
		String apiToken = resolveApiToken();

		if (apiToken == null || apiToken.isEmpty()) {
			log.info("Please configure your OldSchoolDB plugin key in the plugin settings");
			log.info("Get your plugin key from: oldschooldb.com/plugin/setup");
			return;
		}

		authenticationAttempted = true;
		
		authService.authenticateToken(apiToken).thenAccept(error -> {
			isAuthenticated = (error == null);
			if (error == null) {
				log.info("Authenticated with OldSchoolDB");
				if (client.getGameState() == GameState.LOGGED_IN) {
					clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
						"OldSchoolDB: Connected and authenticated!", null));
					clientThread.invoke(this::syncCurrentAccountState);
				} else {
					showAuthMessageOnLogin = true;
				}
			} else {
				log.warn("Auth failed: {}", error);
				authenticationAttempted = false;
				clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"OldSchoolDB: Auth failed - " + error, null));
			}
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			// LOGGED_IN fires again on every map-region load, so only treat the first
			// one of a session as a real login (greeting + full resync).
			boolean freshLogin = !loggedInThisSession;
			loggedInThisSession = true;

			if (freshLogin) {
				// Defer the greeting/auth chat to onGameTick, once the player is loaded.
				pendingGreeting = true;
			}

			if (!isAuthenticated && authService != null) {
				attemptAuthentication();
			}

			if (freshLogin && isAuthenticated && currentAccountHash != null && currentAccountHash != -1L) {
				syncCurrentAccountState();
			}
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.CONNECTION_LOST)
		{
			// Back at the login screen (or dropped) — arm the greeting for next login.
			loggedInThisSession = false;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (!isAuthenticated || currentAccountHash == null || currentAccountHash == -1L) {
			return;
		}
		// Mark dirty and let onGameTick flush on a throttle. Quest completions grant
		// XP, so a dirty skills change also flags a (much less frequent) quest re-scan.
		// Slayer is handled by onConfigChanged, so it is intentionally not touched here.
		skillsDirty = true;
		questsDirty = true;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Re-authenticate when the plugin key changes so a fresh key takes effect
		// immediately without toggling the plugin or relogging. Handled before the
		// auth guard below since it must run even while we're not yet authenticated.
		if ("oldschooldb".equals(event.getGroup()) && "apiToken".equals(event.getKey())) {
			isAuthenticated = false;
			authenticationAttempted = false;
			if (authService != null) {
				attemptAuthentication();
			}
			return;
		}

		if (!isAuthenticated || currentAccountHash == null || currentAccountHash == -1L) {
			return;
		}

		if ("timetracking".equals(event.getGroup())) {
			syncTimeTrackingData();
		} else if (SlayerConfig.GROUP_NAME.equals(event.getGroup())) {
			syncSlayerTaskData();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (!isAuthenticated || currentAccountHash == null || currentAccountHash == -1L) {
			return;
		}

		int varbitId = event.getVarbitId();
		if (varbitId == VarbitID.PRAYER_RIGOUR_UNLOCKED
			|| varbitId == VarbitID.PRAYER_AUGURY_UNLOCKED
			|| varbitId == VarbitID.PRAYER_PRESERVE_UNLOCKED) {
			syncPrayerUnlockData();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Flush the deferred login greeting/auth chat once the player is actually loaded.
		// Doing this here (not on the LOGGED_IN edge) avoids NPEs in other plugins'
		// onChatMessage handlers that read getLocalPlayer() before it's ready.
		if (pendingGreeting && client.getLocalPlayer() != null) {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"OldSchoolDB: Sync active — bank, inventory and gear sync automatically as they change.", null);
			if (isAuthenticated && showAuthMessageOnLogin) {
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"OldSchoolDB: Connected and authenticated!", null);
				showAuthMessageOnLogin = false;
			}
			pendingGreeting = false;
		}

		if (!isAuthenticated || currentAccountHash == null || currentAccountHash == -1L) {
			return;
		}

		// Flush debounced skill/quest syncs on a throttle so bursts of XP don't
		// trigger a send (and quest re-scan) every tick.
		int tick = client.getTickCount();
		if (skillsDirty && tick - lastSkillSyncTick >= SKILL_SYNC_INTERVAL_TICKS) {
			skillsDirty = false;
			lastSkillSyncTick = tick;
			syncSkillData();
		}
		if (questsDirty && tick - lastQuestSyncTick >= QUEST_SYNC_INTERVAL_TICKS) {
			questsDirty = false;
			lastQuestSyncTick = tick;
			syncQuestData();
		}

		// The login-edge syncs fire before getLocalPlayer() is ready, so player_name
		// arrives null. Once the name first becomes available, cache it and push one
		// re-sync so the backend records the character's name.
		if (cachedPlayerName == null) {
			Player local = client.getLocalPlayer();
			String name = local != null ? local.getName() : null;
			if (name != null && !name.isEmpty()) {
				cachedPlayerName = name;
				log.info("Player name now available: '{}' — re-syncing to record it", name);
				syncCurrentAccountState();
			}
		}
	}

	@Subscribe
	public void onAccountHashChanged(AccountHashChanged event)
	{
		currentAccountHash = client.getAccountHash();
		// New account (or account cleared) — drop the cached name so onGameTick
		// re-captures the correct character's name.
		cachedPlayerName = null;
		log.info("Account hash updated: {}", currentAccountHash);
		
		if (currentAccountHash != null && currentAccountHash != -1L && isAuthenticated) {
			syncCurrentAccountState();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		String containerName =
			containerId == InventoryID.BANK.getId() ? "BANK" :
			containerId == InventoryID.INVENTORY.getId() ? "INVENTORY" :
			containerId == InventoryID.EQUIPMENT.getId() ? "EQUIPMENT" : "other(" + containerId + ")";
		boolean willSync = (currentAccountHash != null && currentAccountHash != -1L && isAuthenticated)
			|| ((currentAccountHash == null || currentAccountHash == -1L) && client.getAccountHash() != -1L && isAuthenticated);
		log.info("ItemContainerChanged: {} | authed={} accountHash={} -> {}",
			containerName, isAuthenticated, currentAccountHash, willSync ? "SYNCING" : "SKIPPED (guard failed)");

		// Handle bank, inventory, and equipment changes
		if (event.getContainerId() == InventoryID.BANK.getId()) {
			// Update current account hash when bank changes (in case it wasn't set yet)
			if (currentAccountHash == null || currentAccountHash == -1L) {
				currentAccountHash = client.getAccountHash();
			}
			
			if (currentAccountHash != null && currentAccountHash != -1L && isAuthenticated) {
				syncCurrentBankData();
			}
		} else if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
			// Update current account hash when inventory changes (in case it wasn't set yet)
			if (currentAccountHash == null || currentAccountHash == -1L) {
				currentAccountHash = client.getAccountHash();
			}
			
			if (currentAccountHash != null && currentAccountHash != -1L && isAuthenticated) {
				syncCurrentInventoryData();
			}
		} else if (event.getContainerId() == InventoryID.EQUIPMENT.getId()) {
			// Update current account hash when equipment changes (in case it wasn't set yet)
			if (currentAccountHash == null || currentAccountHash == -1L) {
				currentAccountHash = client.getAccountHash();
			}
			
			if (currentAccountHash != null && currentAccountHash != -1L && isAuthenticated) {
				syncCurrentEquipmentData();
			}
		}
	}

	private void syncCurrentBankData() {
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank == null) {
			return;
		}

		int itemCount = bank.getItems().length;
		authService.sendBankData(currentAccountHash, getPlayerName(), buildItemPayload(bank.getItems()))
			.thenAccept(success -> {
				if (success) {
					log.debug("Bank data synced ({} items) for account: {}", itemCount, currentAccountHash);
				} else {
					log.warn("Failed to sync bank data for account: {}", currentAccountHash);
				}
			});
	}

	private void syncCurrentInventoryData() {
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null) {
			log.info("syncCurrentInventoryData: INVENTORY container is null — bailing, nothing sent");
			return;
		}

		int itemCount = inventory.getItems().length;
		log.info("syncCurrentInventoryData: sending {} items for account={} player={}", itemCount, currentAccountHash, getPlayerName());
		authService.sendInventoryData(currentAccountHash, getPlayerName(), buildItemPayload(inventory.getItems()))
			.whenComplete((success, throwable) -> {
				if (throwable != null) {
					log.error("Inventory sync threw (swallowed before): {}", throwable.toString(), throwable);
				} else if (Boolean.TRUE.equals(success)) {
					log.debug("Inventory data synced ({} items) for account: {}", itemCount, currentAccountHash);
				} else {
					log.warn("Failed to sync inventory data for account: {} (send returned false)", currentAccountHash);
				}
			});
	}

	private void syncCurrentEquipmentData() {
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null) {
			return;
		}

		int equippedCount = 0;
		for (Item item : equipment.getItems()) {
			if (item.getId() != -1) {
				equippedCount++;
			}
		}
		final int count = equippedCount;
		authService.sendEquipmentData(currentAccountHash, getPlayerName(), buildItemPayload(equipment.getItems()))
			.thenAccept(success -> {
				if (success) {
					log.debug("Equipment data synced ({} items) for account: {}", count, currentAccountHash);
				} else {
					log.warn("Failed to sync equipment data for account: {}", currentAccountHash);
				}
			});
	}

	private void syncCurrentAccountState()
	{
		if (!isAuthenticated || currentAccountHash == null || currentAccountHash == -1L) {
			return;
		}

		syncCurrentBankData();
		syncCurrentInventoryData();
		syncCurrentEquipmentData();
		syncSkillData();
		syncQuestData();
		syncPrayerUnlockData();
		syncTimeTrackingData();
		syncSlayerTaskData();
	}

	// Current logged-in character's display name, sent with each sync so the
	// backend can show characters by name instead of raw account hash. Reads the
	// live name when available and caches it; returns the cache otherwise, since
	// getLocalPlayer() is null right after login when the first syncs fire.
	private String getPlayerName()
	{
		Player local = client.getLocalPlayer();
		String name = local != null ? local.getName() : null;
		if (name != null && !name.isEmpty()) {
			cachedPlayerName = name;
		}
		return cachedPlayerName;
	}

	private List<Map<String, Object>> buildItemPayload(Item[] containerItems)
	{
		List<Map<String, Object>> items = new java.util.ArrayList<>();

		for (Item item : containerItems) {
			if (item.getId() <= 0 || item.getQuantity() <= 0) {
				continue;
			}

			Map<String, Object> itemData = new HashMap<>();
			itemData.put("item_id", item.getId());
			itemData.put("quantity", item.getQuantity());

			ItemComposition itemComposition = client.getItemDefinition(item.getId());
			if (itemComposition != null && itemComposition.getName() != null && !itemComposition.getName().trim().isEmpty()) {
				itemData.put("item_name", itemComposition.getName());
			}

			items.add(itemData);
		}

		return items;
	}

	private void syncSkillData()
	{
		Skill[] skills = Skill.values();
		int[] levels = new int[skills.length];
		int[] xp = new int[skills.length];
		for (int i = 0; i < skills.length; i++) {
			levels[i] = client.getRealSkillLevel(skills[i]);
			xp[i] = client.getSkillExperience(skills[i]);
		}

		authService.sendSkillData(currentAccountHash, skills, levels, xp)
			.thenAccept(success -> {
				if (!success) {
					log.warn("Failed to sync skill data for account: {}", currentAccountHash);
				}
			});
	}

	private void syncQuestData()
	{
		// Quest.getState() calls client.runScript() — must be on client thread
		Quest[] quests = Quest.values();
		String[] names = new String[quests.length];
		String[] states = new String[quests.length];
		for (int i = 0; i < quests.length; i++) {
			names[i] = quests[i].getName();
			QuestState state = quests[i].getState(client);
			states[i] = state.name();
		}

		authService.sendQuestData(currentAccountHash, names, states)
			.thenAccept(success -> {
				if (!success) {
					log.warn("Failed to sync quest data for account: {}", currentAccountHash);
				}
			});
	}

	private Map<String, Object> buildPrayerUnlock(String name, int varbitId)
	{
		Map<String, Object> unlock = new HashMap<>();
		unlock.put("name", name);
		unlock.put("varbit_id", varbitId);
		unlock.put("unlocked", client.getVarbitValue(varbitId) == 1);
		return unlock;
	}

	private void syncPrayerUnlockData()
	{
		List<Map<String, Object>> unlocks = new java.util.ArrayList<>();
		unlocks.add(buildPrayerUnlock("Rigour", VarbitID.PRAYER_RIGOUR_UNLOCKED));
		unlocks.add(buildPrayerUnlock("Augury", VarbitID.PRAYER_AUGURY_UNLOCKED));
		unlocks.add(buildPrayerUnlock("Preserve", VarbitID.PRAYER_PRESERVE_UNLOCKED));

		authService.sendPrayerUnlockData(currentAccountHash, unlocks)
			.thenAccept(success -> {
				if (!success) {
					log.warn("Failed to sync prayer unlock data for account: {}", currentAccountHash);
				}
			});
	}

	private void syncTimeTrackingData()
	{
		String profileKey = configManager.getRSProfileKey();
		if (profileKey == null) {
			return;
		}

		List<String> keys = configManager.getRSProfileConfigurationKeys("timetracking", profileKey, "");
		if (keys.isEmpty()) {
			return;
		}

		Map<String, String> entries = new HashMap<>();
		for (String key : keys) {
			String value = configManager.getRSProfileConfiguration("timetracking", key);
			if (value != null) {
				entries.put(key, value);
			}
		}

		authService.sendTimeTrackingData(currentAccountHash, profileKey, entries)
			.thenAccept(success -> {
				if (!success) {
					log.warn("Failed to sync time tracking data for account: {}", currentAccountHash);
				}
			});
	}

	private Integer parseIntegerConfig(String value)
	{
		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void syncSlayerTaskData()
	{
		String taskName = configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.TASK_NAME_KEY);
		String taskLocation = configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.TASK_LOC_KEY);
		Integer initialAmount = parseIntegerConfig(configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.INIT_AMOUNT_KEY));
		Integer remainingAmount = parseIntegerConfig(configManager.getRSProfileConfiguration(SlayerConfig.GROUP_NAME, SlayerConfig.AMOUNT_KEY));

		authService.sendSlayerTaskData(currentAccountHash, taskName, taskLocation, initialAmount, remainingAmount)
			.thenAccept(success -> {
				if (!success) {
					log.warn("Failed to sync Slayer task for account: {}", currentAccountHash);
				}
			});
	}

	@Provides
	OldSchoolDBConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OldSchoolDBConfig.class);
	}

}
