# Surf Stats

A Paper plugin that reads Minecraft's per-player statistics JSON files and persists them to a MySQL/MariaDB database. Statistics are captured on player logout, periodic world saves, and server shutdown.

## Modules

| Module | Description |
|---|---|
| `surf-stats-api` | Public API interfaces and data models |
| `surf-stats-core` | Core implementation (file parsing, database persistence) |
| `surf-stats-paper` | Paper plugin (event listeners, lifecycle management) |

## Requirements

- Paper 1.21.1+
- Java 21+
- MySQL or MariaDB
- [surf-api-paper](https://reposilite.slne.dev) and [surf-core-paper](https://reposilite.slne.dev) plugins

## Building

```bash
./gradlew :surf-stats-paper:build
```

The plugin jar is produced at `surf-stats-paper/build/libs/`.

## Configuration

### config.yml

```yaml
server:
  name: "my-server"      # Unique identifier for this server (must be changed)
  label: "My Server"     # Human-readable display name
```

The plugin will refuse to start if `server.name` is left as `my-server`.

### database.yml

```yaml
logLevel: DEBUG
credentials:
  host: localhost
  port: 3306
  database: my_database
  username: my_user
  password: my_password
pool:
  sizing:
    initialSize: 10
    minIdle: 0
    maxSize: 10
  timeouts:
    maxAcquireTimeMillis: 10000
    maxCreateConnectionTimeMillis: 30000
    maxValidationTimeMillis: -1
    maxIdleTimeMillis: 60000
    maxLifeTimeMillis: 1800000
```

## How It Works

The plugin reads Minecraft's native `<world>/stats/<uuid>.json` files and writes the data to a relational database, attributing each entry to the configured server name.

Statistics are processed at three points:

1. **Player quit** — 1 second after disconnect (gives Minecraft time to flush the stats file)
2. **World save** — 5 seconds after a world save cycle completes (debounced across overworld/nether/end)
3. **Server shutdown** — synchronously during plugin disable, ensuring all online players' stats are saved before the database connection closes

All async processing uses a shared plugin-scoped `CoroutineScope` that is cancelled on shutdown to prevent post-disable coroutine leaks.

## API Usage

Other plugins can access the API through Bukkit's `ServicesManager`:

```kotlin
val api = server.servicesManager.getRegistration(SurfStatsApi::class.java)?.provider
    ?: error("SurfStats not available")

// Load a player's stats
val stats: PlayerStats? = api.getPlayerStats(playerUuid, playerName)

// Access individual stat values
val blocksMined = stats?.getStat("minecraft:mined", "minecraft:stone")

// Get all categories
val categories: Set<String> = stats?.categories() ?: emptySet()
```

### Custom Stats

Other plugins can save custom statistics into the `minecraft:custom` category:

```kotlin
val api = server.servicesManager.getRegistration(SurfStatsApi::class.java)?.provider
    ?: error("SurfStats not available")

// Save a single custom stat
api.saveCustomStat(playerUuid, playerName, "my_plugin:kills", 42L)

// Save multiple custom stats at once (single transaction)
api.saveCustomStats(playerUuid, playerName, mapOf(
    "my_plugin:kills" to 42L,
    "my_plugin:deaths" to 7L
))
```

Both methods are `suspend` functions and must be called from a coroutine context. The database service must be available or an `IllegalStateException` is thrown.

### Key API Types

- **`SurfStatsApi`** — main entry point for reading and processing stats
- **`PlayerStats`** — a player's full statistics (uuid, name, dataVersion, stat entries)
- **`StatEntry`** — a single statistic: category (e.g. `minecraft:mined`), key (e.g. `minecraft:stone`), value
- **`PlayerStatsBatch`** — a `PlayerStats` paired with a server name, ready for database insertion

### Dependency (Gradle)

```kotlin
dependencies {
    compileOnly(project(":surf-stats-api"))
}
```

## Database Schema

The plugin creates and manages these tables:

| Table | Purpose |
|---|---|
| `servers` | Registered server names and labels |
| `players` | Player UUIDs, names, data versions, timestamps |
| `stat_categories` | Unique stat category names (e.g. `minecraft:mined`) |
| `stat_keys` | Unique stat key names (e.g. `minecraft:stone`) |
| `player_stats` | Stat values per player, per key, per server |

## License

SLNE Dev Team
