package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.AccountHashChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "OldSchoolDB Connector",
	description = "Connects your RuneLite to OldSchoolDB for enhanced price tracking"
)
public class ExamplePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ExampleConfig config;

	private AuthService authService;
	private boolean isAuthenticated = false;
	private boolean showAuthMessageOnLogin = false;
	private boolean authenticationAttempted = false;
	private Long currentAccountHash = null;

	@Override
	protected void startUp() throws Exception
	{
		log.info("OldSchoolDB Connector started!");
		authService = new AuthService(config.serverUrl());
		
		// Test connection to server
		authService.testConnection().thenAccept(connected -> {
			if (connected) {
				log.info("Successfully connected to OldSchoolDB server");
				attemptAuthentication();
			} else {
				log.warn("Failed to connect to OldSchoolDB server at: {}", config.serverUrl());
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
		
		if (apiToken.isEmpty()) {
			log.info("Please configure your OldSchoolDB API token in the plugin settings");
			log.info("Get your token from: {}/plugin", config.serverUrl());
			return;
		}

		authenticationAttempted = true;
		
		authService.authenticateToken(apiToken).thenAccept(success -> {
			isAuthenticated = success;
			if (success) {
				log.info("Successfully authenticated with OldSchoolDB using API token");
				if (client.getGameState() == GameState.LOGGED_IN) {
					// Show message immediately if already logged in
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
						"OldSchoolDB: Connected and authenticated!", null);
				} else {
					// Set flag to show message when user logs in
					showAuthMessageOnLogin = true;
				}
			} else {
				log.warn("Failed to authenticate with OldSchoolDB. Please check your API token.");
				log.warn("Get a new token from: {}/plugin", config.serverUrl());
				authenticationAttempted = false; // Allow retry on failure
				if (client.getGameState() == GameState.LOGGED_IN) {
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
						"OldSchoolDB: Authentication failed - check your token!", null);
				}
			}
		});
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

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}

}
