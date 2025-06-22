# OldSchoolDB Connector Plugin

A RuneLite plugin that connects your game client to OldSchoolDB for enhanced price tracking and data syncing.

## Features

- **Bank Syncing**: Automatically sync your bank items and their quantities
- **Inventory Syncing**: Track your inventory items in real-time  
- **Equipment Syncing**: Monitor your currently equipped items
- **Price Tracking**: View current market values for all your items
- **Real-time Updates**: Data syncs automatically when items change

## Setup

1. Configure your API token in the plugin settings
2. Set the server URL (default: http://localhost:3001)
3. Login to RuneScape and the plugin will start syncing automatically

## Configuration

- **API Token**: Get your token from the OldSchoolDB web interface
- **Server URL**: The URL of your OldSchoolDB backend server
- **Welcome Greeting**: Customize the login message

## Usage

The plugin automatically detects changes to your bank, inventory, and equipment and syncs them to your OldSchoolDB account. View your synced data on the web interface at http://localhost:5173/bank-data