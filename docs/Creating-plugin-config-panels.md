Config options are expressed by using @ConfigItem annotations within a subclass of Config.

To get your config to be accessible from the RuneLite settings panel, you will need to flag a getter in your main plugin class with @Provides:

public class MyCoolPlugin extends Plugin {
@Inject
private MyCoolConfig config;

	@Provides
	MyCoolConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MyCoolConfig.class);
	}
In this example, a method with boolean return type will create a checkbox in its associated config panel, which will be checked by default:

@ConfigItem(keyName = "uniqueKey", name = "Display text", description = "Hover text")
default boolean myCheckbox()
{
return true;
}
Now you can @Inject YourConfigClass config elsewhere and call config.myCheckbox() to obtain its current value. RuneLite stores the config settings automatically. Several input widgets are supported for specific types:

image

Somebody write this page properly!