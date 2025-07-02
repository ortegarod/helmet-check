package com.oldschooldb;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.AccountHashChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "OldSchoolDB Connector",
	description = "Connects your RuneLite to OldSchoolDB for enhanced price tracking"
)
public class OldSchoolDBPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OldSchoolDBConfig config;

	@Inject
	private ConfigManager configManager;

	private AuthService authService;
	private boolean isAuthenticated = false;
	private boolean showAuthMessageOnLogin = false;
	private boolean authenticationAttempted = false;
	private Long currentAccountHash = null;

	@Override
	protected void startUp() throws Exception
	{
		System.out.println("OldSchoolDB Connector started!");
		authService = new AuthService("https://api.oldschooldb.com");
		
		// Test connection to server
		authService.testConnection().thenAccept(connected -> {
			if (connected) {
				log.info("Successfully connected to OldSchoolDB server");
				attemptAuthentication();
			} else {
				log.warn("Failed to connect to OldSchoolDB server at: https://api.oldschooldb.com");
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
		
		String apiToken = config.apiToken();
		log.info("API Token length: {}", apiToken != null ? apiToken.length() : "null");
		log.info("API Token preview: {}", apiToken != null && !apiToken.isEmpty() ? 
			apiToken.substring(0, Math.min(10, apiToken.length())) + "..." : "empty");
		
		if (apiToken == null || apiToken.trim().isEmpty()) {
			log.info("Please configure your OldSchoolDB API token in the plugin settings");
			log.info("Get your token from: https://oldschooldb.com/plugin");
			return;
		}

		authenticationAttempted = true;
		
		authService.authenticateToken(apiToken).thenAccept(success -> {
			isAuthenticated = success;
			if (success) {
				log.info("Successfully authenticated with OldSchoolDB using API token");
				// Update status in config panel
				configManager.setConfiguration("oldschooldb", "authStatus", "✓ Verified - Connected");
				
				if (client.getGameState() == GameState.LOGGED_IN) {
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
						"OldSchoolDB: Token verified successfully! ✓", null);
				} else {
					showAuthMessageOnLogin = true;
				}
			} else {
				log.warn("Failed to authenticate with OldSchoolDB. Please check your API token.");
				log.warn("Get a new token from: https://oldschooldb.com/plugin");
				// Update status in config panel
				configManager.setConfiguration("oldschooldb", "authStatus", "✗ Invalid - Check token");
				authenticationAttempted = false; // Allow retry on failure
				
				if (client.getGameState() == GameState.LOGGED_IN) {
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
						"OldSchoolDB: Token verification failed ✗ - Check your token!", null);
				}
			}
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("oldschooldb")) {
			if (event.getKey().equals("apiToken")) {
				log.info("API token changed, verifying...");
				// Update status to show verification in progress
				configManager.setConfiguration("oldschooldb", "authStatus", "⏳ Verifying...");
				authenticationAttempted = false; // Reset to allow new verification
				isAuthenticated = false;
				attemptAuthentication();
			} else if (event.getKey().equals("verifyToken")) {
				boolean shouldVerify = Boolean.parseBoolean(event.getNewValue());
				if (shouldVerify) {
					log.info("Manual token verification requested");
					// Update status to show verification in progress
					configManager.setConfiguration("oldschooldb", "authStatus", "⏳ Verifying...");
					authenticationAttempted = false; // Reset to allow new verification
					isAuthenticated = false;
					attemptAuthentication();
					// Reset the checkbox after verification starts
					configManager.setConfiguration("oldschooldb", "verifyToken", "false");
				}
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", config.greeting(), null);
			
			// Show auth success message if authentication happened before login
			if (isAuthenticated && showAuthMessageOnLogin) {
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
					"OldSchoolDB: Connected and authenticated!", null);
				showAuthMessageOnLogin = false; // Only show once
			}
			
			// Try to authenticate if we haven't already
			if (!isAuthenticated && authService != null) {
				attemptAuthentication();
			}
		}
	}

	@Subscribe
	public void onAccountHashChanged(AccountHashChanged event)
	{
		currentAccountHash = client.getAccountHash();
		log.info("Account hash updated: {}", currentAccountHash);
		
		if (currentAccountHash != null && currentAccountHash != -1L && isAuthenticated) {
			syncCurrentBankData();
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (currentAccountHash != null && currentAccountHash != -1L && isAuthenticated) {
			syncGrandExchangeOffer(event.getSlot(), event.getOffer());
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
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

		authService.sendBankData(currentAccountHash, bank.getItems())
			.thenAccept(success -> {
				if (success) {
					log.debug("Bank data synced successfully for account: {}", currentAccountHash);
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
						"OldSchoolDB: Bank synced (" + bank.getItems().length + " items)", null);
				} else {
					log.warn("Failed to sync bank data for account: {}", currentAccountHash);
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
						"OldSchoolDB: Bank sync failed - check connection", null);
				}
			});
	}

	private void syncCurrentInventoryData() {
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null) {
			return;
		}

		authService.sendInventoryData(currentAccountHash, inventory.getItems())
			.thenAccept(success -> {
				if (success) {
					log.debug("Inventory data synced successfully for account: {}", currentAccountHash);
					// Only show message for inventory if it has items (to avoid spam)
					if (inventory.getItems().length > 0) {
						client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
							"OldSchoolDB: Inventory synced (" + inventory.getItems().length + " items)", null);
					}
				} else {
					log.warn("Failed to sync inventory data for account: {}", currentAccountHash);
				}
			});
	}

	private void syncCurrentEquipmentData() {
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null) {
			return;
		}

		authService.sendEquipmentData(currentAccountHash, equipment.getItems())
			.thenAccept(success -> {
				if (success) {
					log.debug("Equipment data synced successfully for account: {}", currentAccountHash);
					// Count non-null equipped items for the message
					int equippedCount = 0;
					for (Item item : equipment.getItems()) {
						if (item.getId() != -1) {
							equippedCount++;
						}
					}
					if (equippedCount > 0) {
						client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
							"OldSchoolDB: Equipment synced (" + equippedCount + " items)", null);
					}
				} else {
					log.warn("Failed to sync equipment data for account: {}", currentAccountHash);
				}
			});
	}

	private void syncGrandExchangeOffer(int slot, GrandExchangeOffer offer) {
		// Only sync if offer has meaningful data
		if (offer.getItemId() <= 0) {
			return;
		}

		authService.sendGrandExchangeOffer(currentAccountHash, slot, offer)
			.thenAccept(success -> {
				if (success) {
					log.debug("GE offer synced successfully for account: {}, slot: {}", currentAccountHash, slot);
					
					// Show message for significant trades (over 1M gp)
					long tradeValue = (long) offer.getPrice() * offer.getTotalQuantity();
					if (tradeValue >= 1_000_000 && client.getGameState() == GameState.LOGGED_IN) {
						String state = offer.getState().name().toLowerCase();
						client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
							"OldSchoolDB: GE " + state + " synced (" + (tradeValue / 1_000_000) + "M gp)", null);
					}
				} else {
					log.warn("Failed to sync GE offer for account: {}, slot: {}", currentAccountHash, slot);
				}
			});
	}

	@Provides
	OldSchoolDBConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OldSchoolDBConfig.class);
	}

}
