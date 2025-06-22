RuneLite development

Most development takes place on the Plugin Hub, where anyone can submit their own plugin. If you are new to RuneLite development, you likely want to do plugin hub development. Get started here with the example plugin guide: here. You can also contribute directly to existing hub plugins by sending pull requests to the plugin's repository (find those by clicking "Report an issue" on the plugin's page here).

You can also make contributions to the core client. These are less common and are typically reserved for bug fixes and smaller features. The core client development process is different from plugin hub development, and requires building the client first. If you want to make a new feature for the core client, particularly if it is large, start a discussion in #development on Discord to be sure it is a feature we want, rather than doing all of the work up front without communicating any of it first.

Resources for learning plugin development

There are some high level guides on this wiki under the "Developer's Guide" section in the side panel. These cover things like working with git, using the developer tools, and concepts like client vars.

Javadoc for the api and client are also available. However, the best way to get an idea of proper API usage is looking at existing plugins that already do something similar. Otherwise, pick one of the more simple plugins and just read through it, such as e.g. agility, woodcutting, or implings. Or just clone the repository into your IDE which will give you easy search capabilities. You can also look at existing plugin hub plugins, although we recommend this as only a secondary option to looking at core plugins, since their API usage is not always correct.

There are also several external resources for client information:

abex's cache viewer - which views most of the data in the cache in a human-readable way
mejrs's world map - for finding world map coordinates
cs2 scripts - decompiled client scripts
Runescape Wiki tools - e.g. MOID item/npc/object database
OSRS Wiki - after logging in, enable Preferences > Gadgets > Display advanced data in infoboxes, such as item IDs. to display item/npc/object ids
Basic Plugin architecture

Most plugins are comprised of only a few components:

The Plugin class, which extends Plugin and is annotated with @PluginDescriptor (e.g. example plugin Plugin class)
The plugin configuration (extending Config), which generates the plugin's configuration panel (e.g. example plugin Config interface). These settings are saved between client launches. You can also save data programatically by using ConfigManager.
Overlays. Overlays all extend class Overlay, and have a render() method which you must override. Overlays must be registered with the overlay manager on plugin startUp, and unregistered on shutDown. Overlays can draw anything they want anywhere on the game e.g. AbyssOverlay, which draws clickboxes on game objects. Text boxes are made via OverlayPanel (e.g. AttackStylesOverlay). WidgetItemOverlay draws on items.
Event subscribers. These are methods annotated with @Subscribe and must be located in the plugin class. Events power most plugins in RuneLite, and it is how plugins react to things happening in the game, such as objects or npcs spawning, players sending messages, etc. Here is an example of an event subscriber, checking to see if the spawned object is an abyssal rift. There are many events, and some plugins have their own events. Most events you can subscribe to can be found in the api documentation and also client documentation.
Plugin panels. These are Swing panels on the client sidebar, such as the loot tracker and farming tracker. Here is an example of a plugin panel.