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
		name = "Plugin Key",
		description = "Get your plugin key from https://oldschooldb.com/plugin/setup (copy and paste here)",
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
