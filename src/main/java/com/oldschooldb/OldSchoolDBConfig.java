package com.oldschooldb;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("oldschooldb")
public interface OldSchoolDBConfig extends Config
{
	@ConfigItem(
		keyName = "apiToken",
		name = "Plugin Key",
		description = "Paste your key from oldschooldb.com/plugin/setup. Keep it private — it identifies your account.",
		secret = true
	)
	default String apiToken()
	{
		return "";
	}
}
