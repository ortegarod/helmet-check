# OldSchoolDB Connector (RuneLite Plugin)

A RuneLite plugin that connects your game client to **OldSchoolDB** and syncs:
- **Bank**
- **Inventory**
- **Equipment**

This enables the OldSchoolDB web app to show your account/item data (and eventually deeper analytics).

---

## Quickstart (use the hosted Railway backend)

1) **Generate an API token** in the OldSchoolDB web app:
- Log in → go to **`/plugin`** → click **Generate Token**

2) In **RuneLite**:
- Open **Configuration** (wrench)
- Search **“OldSchoolDB Connector”**
- Set:
  - **API Token** = paste token
  - **Server URL** = `https://api.oldschooldb.com`

3) **Verify**
- Log into OSRS and open your bank once.
- You should see chat messages like “Connected and authenticated” and sync confirmations.

---

## Developing the plugin (local)

### Prereqs
- **Java 11** (RuneLite plugins target Java 11)
- IntelliJ IDEA (recommended) or any Java IDE

### Run RuneLite with this plugin enabled

This repo includes a test harness that launches RuneLite with the plugin loaded:
- `src/test/java/com/oldschooldb/OldSchoolDBPluginTest.java`

**In IntelliJ (recommended):**
1. Open the repo
2. Find `OldSchoolDBPluginTest` → Run
3. Edit the run configuration → add VM option: `-ea`

That will launch RuneLite with the plugin loaded.

### Pointing at production vs local

- Production (Railway): `https://api.oldschooldb.com`
- Local backend (if running locally): `http://localhost:3000`

---

## Backend API

The plugin uses API token auth and calls:
- `GET /api/plugin/auth/test`
- `POST /api/plugin/bank/sync`
- `POST /api/plugin/inventory/sync`
- `POST /api/plugin/equipment/sync`

---

## Plugin Hub submission (high level)

RuneLite plugin development typically flows through the **Plugin Hub**.

High-level steps:
1. Ensure this repo is **public** and your plugin metadata is correct (`runelite-plugin.properties`).
2. Fork https://github.com/runelite/plugin-hub
3. Create a new file under `plugins/` containing:
   - `repository=https://github.com/ortegarod/helmet-check.git`
   - `commit=<40-char commit hash>`
4. Open a PR to plugin-hub and follow CI feedback until merged.

More detailed guide: `docs/plugin-hub.md`

## Docs / References

- RuneLite developer guide: https://github.com/runelite/runelite/wiki/Developer-Guide
