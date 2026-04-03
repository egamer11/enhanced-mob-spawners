# Enhanced Mob Spawners

A Minecraft Forge mod that extends vanilla mob spawner functionality with precision controls and enhanced features.

## Features

### 🎯 Precise Spawner
- **Exact mob control**: Spawns exactly 1 mob at a time
- **Smart tracking**: Only spawns new mobs after previous ones die
- **Configurable delays**: Customize spawn timing (1-20 seconds)
- **Mob limits**: Set maximum concurrent mobs per spawner (1-10)
- **Universal compatibility**: Works with ALL spawn eggs (vanilla + modded)
- **Visual effects**: Soul fire flame particles on spawn + spinning mob display
- **Custom texture support**: Unique appearance with transparency

### ⚙️ Spawner Configuration
- **In-game GUI**: Configure spawners with the Spawner Key item
- **Speed control**: Adjust spawn rates (Slow to Very Fast)
- **Range settings**: Customize activation distance (16-128 blocks)
- **Enable/disable**: Turn spawners on/off
- **Redstone control**: Spawners respond to redstone signals

### 🔧 Enhanced Mechanics
- **Silk Touch mining**: Harvest spawners with Silk Touch tools
- **Monster egg drops**: Mobs drop their spawn eggs on death (configurable chance)
- **Spawner upgrading**: Convert regular spawners to Precise Spawners
- **Persistent tracking**: Mob limits maintained across world restarts
- **Right-click egg drop**: Get spawn eggs from existing spawners

## How to Use

### Getting Started
1. **Craft a Spawner Key** (recipe available in JEI/REI)
2. **Right-click any spawner** with the key to open configuration GUI
3. **Adjust settings** using the buttons (Speed, Range, etc.)

### Creating Precise Spawners
1. **Open regular spawner GUI** with Spawner Key
2. **Click "Upgrade to Precise Spawner"** button
3. **Configure precise settings** (Delay, Mob Limit)
4. **Use spawn eggs** to change mob types

### Configuration Options

#### Regular Spawners
- **Speed**: Slow → Default → Fast → Very Fast
- **Range**: Default (16) → Far (32) → Very Far (64) → Extreme (128)
- **Enable/Disable**: Turn spawner on/off

#### Precise Spawners
- **Delay**: Very Fast (1s) → Fast (2.5s) → Default (5s) → Slow (10s) → Very Slow (20s)
- **Mob Limit**: 1-10 concurrent mobs maximum
- **All regular spawner options** also available

### Spawn Egg Compatibility
- **Right-click Precise Spawners** with any spawn egg to change mob type
- **Works with ALL mods** that add spawn eggs
- **Automatic detection** of entity types

## Installation

1. **Download** the mod JAR file
2. **Install Minecraft Forge** 1.20.1 (version 47.1.0+)
3. **Place mod file** in `.minecraft/mods/` folder
4. **Launch game** and enjoy!

## Requirements

- **Minecraft**: 1.20.1
- **Forge**: 47.1.0 or higher
- **Java**: 17+

## Config File

The mod creates a config file at `.minecraft/config/spawnermod.properties` with options for:
- Spawn egg drop chances
- Item blacklists
- Default spawner settings
- Feature toggles

## Tips & Tricks

### Mob Farming
- Use **Precise Spawners** for controlled mob farms
- Set **low delays** for rapid spawning
- **Limit mobs** to prevent overcrowding

### Decoration
- **Disable spawners** for decorative purposes
- Use **different mob types** for themed builds
- **Redstone control** for automated farms

### Performance
- **Lower mob limits** reduce server load
- **Increase delays** for better performance
- **Use range limits** to optimize spawning

## Troubleshooting

### Common Issues
- **Spawner not working**: Check if redstone is powering it (disables spawning)
- **No spawn eggs dropping**: Verify drop chance in config file
- **Can't open GUI**: Make sure you're using the Spawner Key item
- **Mobs not spawning**: Check mob limit and ensure adequate space

### Performance Issues
- Reduce mob limits in Precise Spawners
- Increase spawn delays
- Lower spawn egg drop rates in config

## New in This Version

### Precise Spawner Features
- Brand new block type with exact mob control
- Soul fire particle effects on spawn
- Custom texture and transparent cage design
- Universal spawn egg compatibility
- Persistent mob tracking across world reloads

---

*Enhance your mob spawning experience with precision and control!*
