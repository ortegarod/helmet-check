# OldSchoolDB Connector

A RuneLite plugin that syncs your account to [OldSchoolDB](https://oldschooldb.com) in real time, so you can view, search, and track your account from anywhere.

## What it syncs

Once you add your plugin key, the plugin keeps your OldSchoolDB profile up to date with:

- **Bank** — items and quantities
- **Inventory** — current inventory contents
- **Equipment** — currently equipped gear
- **Skills** — levels and XP
- **Quests** — completion state
- **Slayer** — current task and progress
- **Prayer unlocks** — Rigour, Augury, and Preserve

Your data syncs automatically whenever it changes. On the [OldSchoolDB](https://oldschooldb.com) website you can then browse your items, see current market values, and track your account over time.

> **Privacy:** The plugin only sends **your own** account data, and only to **your** OldSchoolDB account at `api.oldschooldb.com`. Nothing is sent until you enter a valid plugin key — clear the key to stop syncing.

## Setup

### 1. Create an account

Sign up at [oldschooldb.com](https://oldschooldb.com).

### 2. Get your plugin key

Go to [oldschooldb.com/plugin/setup](https://oldschooldb.com/plugin/setup), generate a **plugin key**, and copy it.

> **Note:** Keep your plugin key private — it links game data to your account. If you ever expose it, generate a new one to revoke the old key.

### 3. Add the key to the plugin

In RuneLite: open **Configuration** (wrench icon) → search **OldSchoolDB Connector** → paste your key into the **Plugin Key** field.

### 4. Verify

Log into RuneScape. When the key is valid you'll see **"OldSchoolDB: Connected and authenticated!"** in your chat, and your account starts syncing. View it at [oldschooldb.com](https://oldschooldb.com).

## Support

Found a bug or have a feature request? [Open an issue](https://github.com/ortegarod/oldschooldb-runelite-plugin/issues) on GitHub.
