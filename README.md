# PrismPack

A feature-rich backpack plugin for Minecraft servers that supports both MySQL and SQLite databases. Players can store and manage items in their personal backpacks with customizable sizes, sounds, and restrictions.

## Features

- MySQL and SQLite support
- Customizable GUI and sounds
- Permission-based backpack sizes
- Item blacklisting
- Cooldown system
- Death handling (keep items on death)
- Admin commands for managing other players' backpacks
- Multi-language support through lang.yml

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/backpack` | `backpack.use` | Open your backpack |
| `/bp reload` | `prismpack.command.reload` | Reload plugin configuration |
| `/bp clean` | `backpack.clean` | Empty your backpack |
| `/bp clean <player>` | `backpack.clean.others` | Empty another player's backpack |
| `/bp open <player>` | `backpack.open.others` | View/edit another player's backpack |

Aliases: `/prismpack`, `/bp`

## Permissions

| Permission | Description |
|------------|-------------|
| `backpack.use` | Allow use of backpack |
| `backpack.noCooldown` | Bypass cooldown restriction |
| `backpack.keepOnDeath` | Keep backpack items on death |
| `backpack.size.1` | 1 row (9 slots) |
| `backpack.size.2` | 2 rows (18 slots) |
| `backpack.size.3` | 3 rows (27 slots) |
| `backpack.size.4` | 4 rows (36 slots) |
| `backpack.size.5` | 5 rows (45 slots) |
| `backpack.size.6` | 6 rows (54 slots) |

## Configuration

### config.yml 

```yaml
mysql:
  host: "localhost"
  port: 3306
  database: "database"
  username: "root"
  password: "password"
  poolsize: 10

sqlite:
  enabled: true
  path: "database.db"

BackPackGUI:
  name: "%player% <red> backpack"
  sound:
    name: "ENTITY_SHULKER_OPEN"
    volume: 1.0
    pitch: 1.0
  close_sound:
    name: "ENTITY_SHULKER_OPEN"
    volume: 1.0
    pitch: 1.0
  cooldown: 30 # Cooldown in seconds
  blocked-items:
    - "SHULKER_BOX"
    - "WHITE_SHULKER_BOX"
    - "BLACK_SHULKER_BOX"
```

### lang.yml

```yaml
messages:
  onCooldown: "<red>You must wait <yellow>%time_left% seconds <red>before opening your backpack again."
  openedBackpack: "<green>Opened your backpack."
  noBackpack: "<red>You do not have a backpack."
  cleanedBackpack: "<green>Your backpack has been cleaned."
  cleanedOthersBackpack: "<green>Cleaned the backpack of %player%."
  playerNotFound: "<red>Player not found."
  blacklistedItem: "<red>You cannot store this item in your backpack."
  openedBackpackOther: "<green>Opened the backpack of %player%."
  reloadedConfig: "<green>Config reloaded."
```

## Installation

1. Download the latest release
2. Place the jar file in your plugins folder
3. Start/restart your server
4. Configure the plugin in `config.yml` and `lang.yml`
5. Use `/bp reload` to apply changes

## Database Setup

### MySQL
1. Create a MySQL database
2. Configure the MySQL settings in config.yml
3. Set `sqlite.enabled` to `false`
4. Restart the server

### SQLite
1. Set `sqlite.enabled` to `true` in config.yml
2. Configure the desired database file path
3. Restart the server

## Support

If you encounter any issues or have suggestions:

- [Create an issue](https://github.com/Person98/PrismPack/issues)
- Join our [Discord](https://discord.gg/minewebinnovations)
- Visit our [Website](https://minewebinnovations.com)

## Downloads

Latest builds can be found on our Jenkins:
[ci.minewebinnovations.com/job/PrismPack](https://ci.minewebinnovations.com/job/PrismPack/)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.