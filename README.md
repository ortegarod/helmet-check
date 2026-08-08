# OldSchoolDB Connector Plugin

A RuneLite plugin that connects your game client to OldSchoolDB for enhanced price tracking and data syncing.

## Features

- **Bank Syncing**: Automatically sync your bank items and their quantities
- **Inventory Syncing**: Track your inventory items in real-time  
- **Equipment Syncing**: Monitor your currently equipped items
- **Price Tracking**: View current market values for all your items
- **Real-time Updates**: Data syncs automatically when items change

## Dev Setup (macOS ARM64)

### Requirements
- Java 11 via jenv
- IntelliJ IDEA Community Edition

### Steps

1. Set Java 11 for this directory:
```bash
cd path/to/oldschooldb/plugin
jenv local 11
```

2. Open this folder in IntelliJ: File → Open → `plugin/`

3. Go to Run → Edit Configurations → click `+` → Application → set:
   - **Main class:** `com.oldschooldb.OldSchoolDBPluginTest`
   - **VM options:** `-ea`
   - **Use classpath of module:** select the plugin module

4. Click OK, then Run — RuneLite launches with the plugin loaded

## Plugin Setup

### 1. Get your plugin token

Go to the OldSchoolDB web interface → **/plugin** page → **RuneLite Plugin Token** section → click **Generate Plugin Token** → copy it (shown once).

> There are two token types on that page. The **RuneLite Plugin Token** is what goes in the plugin. **Developer API Keys** are for external tools (like AI companions) to read your data — don't mix them up.

### 2. Configure RuneLite

In RuneLite: open **Configuration** (wrench icon) → search **OldSchoolDB Connector** → set:

- **API Token** — paste your plugin token
- **Server URL** — see below
- **Welcome Greeting** — optional, customize your login message

### Server URL

| Environment | URL |
|-------------|-----|
| Local dev | `http://localhost:3001` |
| Production | `https://api.oldschooldb.com` |

### 3. Login and verify

Log into RuneScape. The plugin syncs your bank, inventory, and equipment automatically whenever they change.

- **Local:** view synced data at `http://localhost:5173/bank-data`
- **Production:** view at the OldSchoolDB frontend

## Troubleshooting

### If you see missing lwjgl jars

```
Could not find lwjgl-3.3.2-natives-linux-arm64.jar
```

Run this:
```bash
curl -o ~/.m2/repository/org/lwjgl/lwjgl/3.3.2/lwjgl-3.3.2-natives-linux-arm64.jar \
  "https://repo.runelite.net/org/lwjgl/lwjgl/3.3.2/lwjgl-3.3.2-natives-linux-arm64.jar"

curl -o ~/.m2/repository/org/lwjgl/lwjgl-opengl/3.3.2/lwjgl-opengl-3.3.2-natives-linux-arm64.jar \
  "https://repo.runelite.net/org/lwjgl/lwjgl-opengl/3.3.2/lwjgl-opengl-3.3.2-natives-linux-arm64.jar"
```