package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
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
	}

	private void attemptAuthentication() {
		String apiToken = config.apiToken();
		
		if (apiToken.isEmpty()) {
			log.info("Please configure your OldSchoolDB API token in the plugin settings");
			log.info("Get your token from: {}/plugin", config.serverUrl());
			return;
		}

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

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}

}
