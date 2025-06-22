package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface ExampleConfig extends Config
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
		description = "Get your token from: http://localhost:3001/plugin (copy and paste here)",
		secret = true
	)
	default String apiToken()
	{
		return "";
	}

	@ConfigItem(
		keyName = "serverUrl",
		name = "Server URL",
		description = "OldSchoolDB server URL"
	)
	default String serverUrl()
	{
		return "http://localhost:3001";
	}
}
