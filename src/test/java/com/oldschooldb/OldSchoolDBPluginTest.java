package com.oldschooldb;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OldSchoolDBPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OldSchoolDBPlugin.class);
		RuneLite.main(args);
	}
}