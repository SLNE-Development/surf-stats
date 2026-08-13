# Surf Stats

A Paper plugin + standalone microservice that captures Minecraft per-player statistics and persists them to a relational database. The Paper plugin reads Minecraft's native stats JSON files and ships absolute current values; the microservice owns all database access (including server-side delta computation) and the two communicate over RabbitMQ.

## Modules

| Module | Description |
|---|---|
| `surf-stats-api` | Public API interfaces and serializable data models |
| `surf-stats-core/surf-stats-core-common` | Shared RabbitMQ packets and opt-out mappings |
| `surf-stats-core/surf-stats-core-client` | Client-side implementation (file parsing, packet sending) |
| `surf-stats-paper` | Paper plugin (event listeners, periodic save, commands, opt-out menu) |
| `surf-stats-microservice` | Standalone microservice that handles RabbitMQ packets and writes to the database |

## Requirements

### Paper plugin
- Paper 1.21+ (Folia is supported)
- Java 21+
- [`surf-rabbitmq-paper`](https://reposilite.slne.dev) — required
- [`surf-clan-paper`](https://reposilite.slne.dev) — soft dependency (used to attribute diff entries to clans)
- `surf-api-paper` and `surf-core-paper` (transitive)

### Microservice
- Java 21+
- A RabbitMQ broker reachable by both the plugin and the microservice
- An R2DBC-compatible database (configured via the bundled `surf-database` integration)

## Building

```bash
./gradlew build
```

Output artifacts:
- Plugin jar: `surf-stats-paper/build/libs/`
- Microservice jar: `surf-stats-microservice/build/libs/`

## Configuration

Surf Stats does not ship its own `config.yml` or `database.yml`. Configuration is delegated to the underlying infrastructure plugins:

- **Server identity** is provided by `surf-core-paper` (`SurfCoreApi.getCurrentServerName()` / `getCurrentServerDisplayName()`). The plugin will register itself in the `servers` table on startup.
- **RabbitMQ connection** is configured by `surf-rabbitmq-paper` on the plugin side and by the bundled `ServerRabbitMQApi` on the microservice side.
- **Database connection** is owned by the microservice via `surf-database` (R2DBC).

## How It Works

```
┌────────────────┐   stats JSON    ┌───────────────────┐    RabbitMQ     ┌──────────────────┐
│ Minecraft      │ ──────────────► │ surf-stats-paper  │ ──────────────► │ surf-stats-      │
│ <world>/stats/ │                 │  (read & ship)    │                 │ microservice     │
└────────────────┘                 └───────────────────┘                 │ (delta + persist)│
                                                                         └──────────────────┘
```

The Paper plugin reads Minecraft's native `<world>/stats/<uuid>.json` files and ships absolute current values to the microservice over RabbitMQ. The microservice owns the database, computes per-tuple deltas against the `player_stats.last_diff_value` baseline, and writes both the current row and the history row in a single transaction.

It also reads `<world>/advancements/<uuid>.json` and ships complete advancement snapshots. Recipe advancements (`recipes/…`) are filtered out client-side — they outnumber real advancements roughly ten to one. No history is kept for advancements: the microservice replaces everything stored for `(player, server)` inside one transaction, so revoked advancements disappear from the database as well.

Statistics are processed at four points:

1. **Player join** — loads the player's initial snapshot into memory.
2. **Player quit** — 1 second after disconnect (gives Minecraft time to flush the stats file), computes final diffs and saves.
3. **Periodic** — every 5 minutes, all tracked players are flushed to disk and saved.
4. **Server shutdown** — final flush + save for all tracked players before disconnect.

Advancements are processed at three points:

1. **Player quit** — in the same 1 second delayed pass as the statistics.
2. **World save** — on `WorldSaveEvent`, debounced to at most once every 30 seconds since the event fires once per world.
3. **Server shutdown** — final flush + save for all tracked players.

A per-player content hash suppresses advancement sends when the snapshot is unchanged since the last successful send, so most world saves ship nothing.

All async work uses Folia-compatible coroutines (`mccoroutine-folia`) and a plugin-scoped `CoroutineScope` that is cancelled on shutdown.

### Current vs. diff stats

There are two parallel persistence paths:

- **`PlayerStatsTable`** (`player_stats`) — UPSERTed on `(player_uuid, category, key, server_name)`. The `value` column always reflects the current absolute value (written by `saveStats`); the `last_diff_value` column tracks the per-tuple baseline used for diff computation (written by `saveDiffStats`).
- **`PlayerStatsHistoryTable`** (`player_stats_history`) — append-only INSERTs of deltas with `created_at` timestamp and optional `clan_uuid`. Suitable for time-series analysis. Deltas are computed server-side: `delta = entry.value - last_diff_value`. Only rows with `delta > 0` are inserted; the baseline is always advanced to the new value.

## API Usage

The API is exposed as a Bukkit service. Use the helpers from `surf-api-core`:

```kotlin
import dev.slne.surf.stats.api.SurfStatsApi

// Companion object delegates to the registered service
SurfStatsApi.processPlayerStats(playerUuid)
val stats = SurfStatsApi.getPlayerStats(playerUuid)
```

### Interface

```kotlin
interface SurfStatsApi {
    suspend fun processPlayerStats(playerUuid: UUID)
    suspend fun processAllPlayerStats(uuids: Set<UUID>)
    suspend fun getPlayerStats(playerUuid: UUID): PlayerStats
    suspend fun saveStats(playerUuid: UUID, stats: PlayerStats)
    suspend fun saveDiffStats(playerUuid: UUID, stats: PlayerStats)
    suspend fun getPlayerAdvancements(playerUuid: UUID): PlayerAdvancements
    suspend fun saveAdvancements(playerUuid: UUID, advancements: PlayerAdvancements)
    suspend fun processPlayerAdvancements(playerUuid: UUID)
    suspend fun processAllPlayerAdvancements(uuids: Set<UUID>)
}
```

| Method | Purpose |
|---|---|
| `processPlayerStats` | Load current stats for one player and ship them on both the current and diff paths |
| `processAllPlayerStats` | Same as above, batched in parallel for many players |
| `getPlayerStats` | Load the player's current absolute stats from disk |
| `saveStats` | Send a `CURRENT`-type batch (UPSERT `player_stats.value`) |
| `saveDiffStats` | Send a `DIFFERENCE`-type batch — pass **absolute current values**; the microservice computes the delta against `last_diff_value` and appends to `player_stats_history` |
| `getPlayerAdvancements` | Load the player's current advancement snapshot from disk (recipes excluded) |
| `saveAdvancements` | Replace everything stored for `(player, server)` with the given snapshot — must be complete; empty snapshots are ignored |
| `processPlayerAdvancements` | Load one player's advancements and send them if they changed |
| `processAllPlayerAdvancements` | Same, batched for many players |

> **Breaking change:** `saveDiffStats` now expects absolute current values, not pre-computed deltas. Sending deltas under the new contract will make the per-tuple baseline drift and corrupt history. The first call per `(player, category, key, server)` writes `entry.value` as the first delta (baseline starts at 0).

All methods are `suspend`. Callers must run them inside a coroutine scope (e.g. `mccoroutine`'s `plugin.launch { … }`).

### Custom Stats

Other plugins can persist their own statistics through `saveStats` and `saveDiffStats`. The microservice does not whitelist categories or keys — unknown ones are auto-registered into `stat_categories` / `stat_keys`.

```kotlin
import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.StatEntry

val customStats = PlayerStats(
    playerUuid = playerUuid,
    serverName = SurfCoreApi.getCurrentServerName(),
    stats = listOf(
        StatEntry(key("myplugin:economy"), key("coins_earned"), 150L),
        StatEntry(key("myplugin:economy"), key("coins_spent"), 80L),
    )
)

// "Current value" semantics — overwrites previous row for the same (player, category, key, server)
SurfStatsApi.saveStats(playerUuid, customStats)

// "Event" semantics — pass absolute values; the microservice computes the delta
// against last_diff_value and appends a history row when delta > 0.
SurfStatsApi.saveDiffStats(playerUuid, customStats)
```

**Things to know:**

- Categories and keys are `net.kyori.adventure.key.Key` (namespaced). Use any namespace you own (e.g. `myplugin:`).
- Values are `Long` only.
- Player opt-outs (`player_stat_optouts`) apply to custom stats too — opted-out `(category, key)` pairs are filtered out silently before insertion.
- For custom stats, pass the absolute current value to either method. Pre-computing deltas client-side and shipping them via `saveDiffStats` is no longer supported and will corrupt the per-tuple baseline.
- The `serverName` you pass must reference a registered server in the `servers` table. Use `SurfCoreApi.getCurrentServerName()` to stay consistent.

### Key API Types

- **`SurfStatsApi`** — service interface; access via the companion object or `requiredService<SurfStatsApi>()`.
- **`PlayerStats`** — a player's stats for one server: `playerUuid`, `serverName`, `List<StatEntry>`. Implements `Collection<StatEntry>`.
- **`StatEntry`** — `category: SerializableKey`, `key: SerializableKey`, `value: Long`.
- **`PlayerStatsBatch`** — internal RabbitMQ payload (player + stats + server + optional clan).
- **`OptOutInfo` / `OptOutType`** — opt-out records and IN/OUT toggles.

### Dependency (Gradle)

```kotlin
dependencies {
    compileOnly(project(":surf-stats-api"))
}
```

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/stats` | `surf.stats.command.generic` | Root command |
| `/stats optout` | `surf.stats.command.opt-out` | Opens the opt-out menu where players can disable specific statistics |

## Database Schema

The microservice reads and writes these tables:

| Table | Purpose |
|---|---|
| `servers` | Registered server names, labels, active flag |
| `stat_categories` | Distinct stat category keys (e.g. `minecraft:mined`, `myplugin:economy`) |
| `stat_keys` | Distinct stat keys (e.g. `minecraft:stone`, `coins_earned`) |
| `player_stats` | Current absolute `value` per `(player, category, key, server)` plus `last_diff_value` baseline used for server-side delta computation — UPSERT |
| `player_stats_history` | Append-only delta entries with `created_at` + optional `clan_uuid` |
| `player_stat_optouts` | Per-player opt-out list of `(category, key)` pairs to exclude from persistence |
| `advancements` | Distinct advancement identifiers (e.g. `minecraft:story/root`) |
| `player_advancements` | Current advancement state per `(player, advancement, server)` — `done`, `completed_at`, `criteria_done` — replaced as a whole snapshot |
| `player_advancement_criteria` | Awarded criteria per advancement with `awarded_at` |

The three advancement tables must be created manually before deploying this version. DDL for them is in `docs/superpowers/specs/2026-08-09-advancement-sync-design.md`.

## License

SLNE Dev Team
