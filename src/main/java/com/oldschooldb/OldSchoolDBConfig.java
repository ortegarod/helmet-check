package com.oldschooldb;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("oldschooldb")
public interface OldSchoolDBConfig extends Config
{
	@ConfigItem(
		keyName = "greeting",
		name = "Welcome Greeting",
		description = "The message to show to the user when they login"
	)
	default String greeting()
	{
		return "OldSchoolDB: Bank sync ready! Open your bank to track items.";
	}

	@ConfigItem(
		keyName = "apiToken",
		name = "API Token", 
		description = "Get your token from: https://oldschooldb.com/plugin - Token will be automatically verified when you paste it here",
		secret = true
	)
	default String apiToken()
	{
		return "";
	}

	@ConfigItem(
		keyName = "verifyToken",
		name = "Verify Token Now",
		description = "Check this box to verify your API token immediately"
	)
	default boolean verifyToken()
	{
		return false;
	}

	@ConfigItem(
		keyName = "authStatus",
		name = "Authentication Status",
		description = "Shows whether your API token is valid"
	)
	default String authStatus()
	{
		return "Not verified";
	}

	@ConfigItem(
		keyName = "useLocalhost",
		name = "Use Localhost (Development)",
		description = "Connect to localhost:3001 instead of production server for testing"
	)
	default boolean useLocalhost()
	{
		return false;
	}

}
