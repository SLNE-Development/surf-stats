# Advancement Synchronisation — Design

**Date:** 2026-08-09
**Status:** Approved (design), pending implementation plan

## Summary

Surf Stats currently synchronises only player statistics (`<world>/stats/<uuid>.json`). This
design adds synchronisation of player **advancements** (`<world>/advancements/<uuid>.json`)
along the same path: the Paper plugin reads the native JSON file and ships the state over
RabbitMQ, the microservice owns the database.

No history is kept. The database always reflects the *current* advancement state per
`(player, server)`. Synchronisation happens on player quit, on world save, and on server
shutdown.

## Source data format

`<world>/advancements/<uuid>.json` is a flat JSON object whose keys are advancement
identifiers, plus one non-advancement key `DataVersion`:

```json
{
  "minecraft:adventure/adventuring_time": {
    "criteria": {
      "minecraft:plains": "2024-05-15 16:30:56 +0000",
      "minecraft:desert": "2024-05-16 09:02:11 +0000"
    },
    "done": false
  },
  "minecraft:story/root": {
    "criteria": { "crafting_table": "2024-05-15 16:04:02 +0000" },
    "done": true
  },
  "DataVersion": 4189
}
```

- `criteria` maps a criterion name to the timestamp at which it was awarded. Only *awarded*
  criteria appear — the total number of criteria of an advancement is **not** in this file.
- `done` indicates whether the advancement itself is complete.
- Criterion names are arbitrary strings (`has_log`, `in_bed`, `minecraft:plains`), **not**
  guaranteed to be valid `ResourceLocation`s. Datapacks may use any name.
- Timestamp format: Vanilla writes `yyyy-MM-dd HH:mm:ss Z`. It could not be verified whether
  newer versions emit ISO-8601 instead, so the parser accepts both (see *Assumptions*).

A vanilla player file holds roughly 1,100 `minecraft:recipes/…` entries against roughly 120
real advancements. Recipe advancements are a gameplay mechanic, not a statistic, and are
filtered out client-side. The filter matches on the key *value* (`recipes/…`) regardless of
namespace, so datapack recipe advancements are dropped as well.

## Scope decisions

| Decision | Choice |
|---|---|
| Granularity | Advancement header rows **and** per-criterion detail rows |
| Recipe advancements | Filtered out (key *value* starts with `recipes/`, any namespace) |
| Trigger points | `PlayerQuitEvent`, `WorldSaveEvent` (debounced), plugin shutdown |
| Opt-out | Not applicable to advancements — always synchronised |
| History | None. Current state only |
| Reconciliation | Full snapshot replace per `(player, server)` |
| `criteria_total` | Deferred, see *Deferred* |

## Database schema

Three new tables in `surf-stats-microservice`, following the existing conventions
(`nativeUuid` for UUID columns, `Key` transform for namespaced identifiers, foreign keys onto
dimension tables).

```
advancements                          -- dimension table, mirrors stat_keys
  name              VARCHAR(255)  PK  -- "minecraft:adventure/adventuring_time"

player_advancements
  player_uuid       UUID          ┐
  advancement_name  VARCHAR(255)  ├─ PK   → FK advancements.name
  server_name       VARCHAR(128)  ┘       → FK servers.name
  done              BOOLEAN
  completed_at      TIMESTAMP NULL        -- max(awarded_at) when done, else NULL
  criteria_done     INT                   -- number of awarded criteria
  updated_at        TIMESTAMP

player_advancement_criteria
  player_uuid       UUID          ┐
  advancement_name  VARCHAR(255)  ├─ PK   → FK advancements.name
  criterion_name    VARCHAR(128)  │
  server_name       VARCHAR(128)  ┘       → FK servers.name
  awarded_at        TIMESTAMP NULL
```

Rationale for the non-obvious choices:

- **`criterion_name` is a plain `varchar`, not a `Key`.** Criterion names are free-form
  strings; `Key.key()` throws on uppercase or otherwise non-conforming input, which would
  abort the parse of an entire file.
- **`VARCHAR(255)` instead of the 128 used elsewhere.** The longest vanilla recipe
  advancement identifiers reach ~100 characters; datapack identifiers can exceed 128.
- **`awarded_at` is nullable.** If a timestamp cannot be parsed, the criterion is still
  recorded as awarded rather than dropped.
- **`completed_at` and `criteria_done` are derived, not transmitted.** The microservice
  computes them when writing.
- **`updated_at` is snapshot-scoped, not row-scoped.** Because a snapshot is written as a
  whole (delete + insert) and only written when it changed, every row of a player carries the
  timestamp of the last change to *that player's* advancement state — not of the individual
  advancement. `completed_at` is the per-advancement timestamp.

Indexes on `player_advancements`:

- `idx_player_advancements_player_done` on `(player_uuid, done)` — "which advancements has
  this player completed".
- `idx_player_advancements_advancement_done` on `(advancement_name, done, completed_at)` —
  "who completed this advancement, and when" (leaderboards).

### Table objects

Defined in `dev.slne.surf.stats.microservice.db.tables`, one file per table, matching the
existing style:

```kotlin
object AdvancementsTable : Table("advancements") {
    val name = varchar("name", 255).transform({ key(it) }, { it.asString() })
    override val primaryKey = PrimaryKey(name)
}
```

`PlayerAdvancementsTable` and `PlayerAdvancementCriteriaTable` follow the same pattern with
`.references(AdvancementsTable.name)` / `.references(ServersTable.name)` on their foreign key
columns.

### DDL

The repository has no schema-migration mechanism — `DatabaseApi.create()` does not create
tables, and no SQL or migration files exist in version control. The existing tables were
created out of band, and the operator creates these three the same way, by hand, before the
new microservice build goes live.

PostgreSQL DDL. `nativeUuid` maps to the native `UUID` type, and Exposed's `timestamp()`
maps to `TIMESTAMP` without time zone — matching the existing `servers.created_at` and
`player_stats_history.created_at` columns.

```sql
CREATE TABLE advancements (
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_advancements PRIMARY KEY (name)
);

CREATE TABLE player_advancements (
    player_uuid      UUID         NOT NULL,
    advancement_name VARCHAR(255) NOT NULL,
    server_name      VARCHAR(128) NOT NULL,
    done             BOOLEAN      NOT NULL DEFAULT FALSE,
    completed_at     TIMESTAMP    NULL,
    criteria_done    INTEGER      NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_player_advancements
        PRIMARY KEY (player_uuid, advancement_name, server_name),
    CONSTRAINT fk_player_advancements_advancement
        FOREIGN KEY (advancement_name) REFERENCES advancements (name),
    CONSTRAINT fk_player_advancements_server
        FOREIGN KEY (server_name) REFERENCES servers (name)
);

CREATE INDEX idx_player_advancements_player_done
    ON player_advancements (player_uuid, done);
CREATE INDEX idx_player_advancements_advancement_done
    ON player_advancements (advancement_name, done, completed_at);

CREATE TABLE player_advancement_criteria (
    player_uuid      UUID         NOT NULL,
    advancement_name VARCHAR(255) NOT NULL,
    criterion_name   VARCHAR(128) NOT NULL,
    server_name      VARCHAR(128) NOT NULL,
    awarded_at       TIMESTAMP    NULL,
    CONSTRAINT pk_player_advancement_criteria
        PRIMARY KEY (player_uuid, advancement_name, criterion_name, server_name),
    CONSTRAINT fk_player_advancement_criteria_advancement
        FOREIGN KEY (advancement_name) REFERENCES advancements (name),
    CONSTRAINT fk_player_advancement_criteria_server
        FOREIGN KEY (server_name) REFERENCES servers (name)
);
```

Both delete predicates (`WHERE player_uuid = ? AND server_name = ?`) are served by the
leading `player_uuid` column of each primary key; no additional index is needed.

## API models (`surf-stats-api`)

```kotlin
@Serializable
data class AdvancementCriterion(
    val name: String,
    val awardedAt: SerializableInstant?
)

@Serializable
data class AdvancementEntry(
    val advancement: SerializableKey,
    val done: Boolean,
    val criteria: List<AdvancementCriterion> = emptyList()
) {
    val completedAt: Instant?
        get() = if (done) criteria.mapNotNull { it.awardedAt }.maxOrNull() else null
    val criteriaDone: Int get() = criteria.size
}

@Serializable
data class PlayerAdvancements(
    val playerUuid: SerializableUUID,
    val serverName: String,
    val advancements: List<AdvancementEntry> = emptyList()
) : Collection<AdvancementEntry>
```

`completedAt` and `criteriaDone` are computed properties without backing fields, so
kotlinx.serialization does not put them on the wire.

There is no `Batch` wrapper. `PlayerStatsBatch` exists only to carry `clanUuid`, which
advancements do not need.

### Service interface

Four methods are added to the existing `SurfStatsApi` service, so the plugin keeps exposing a
single Bukkit service:

```kotlin
suspend fun getPlayerAdvancements(playerUuid: UUID): PlayerAdvancements
suspend fun saveAdvancements(playerUuid: UUID, advancements: PlayerAdvancements)
suspend fun processPlayerAdvancements(playerUuid: UUID)
suspend fun processAllPlayerAdvancements(uuids: Set<UUID>)
```

## Packet (`surf-stats-core-common`)

```kotlin
@Serializable
data class SaveAdvancementsRequestPacket(
    val players: List<PlayerAdvancements>
) : RabbitRequestPacket<StatsUuidResponsePacket>()
```

The response reuses the existing `StatsUuidResponsePacket`, which carries the UUIDs of players
whose write failed.

## Components

### `surf-stats-core-client`

| Component | Responsibility |
|---|---|
| `json/AdvancementsJsonModel` | Parses the raw JSON object into `AdvancementEntry` values; drops `DataVersion`; filters `recipes/`; skips malformed entries |
| `json/AdvancementsFileService` + `Impl` | Locates and reads `advancements/<uuid>.json`; mirrors `StatsFileService` (`initialize`, `loadAdvancements`, `loadAllAdvancements`, `getAdvancementsFilePath`, `advancementsExist`) |
| `repository/PlayerAdvancementsRepository` + `Impl` | Turns `Result` failures into empty snapshots and logs them; mirrors `PlayerStatsRepository` |
| `service/AdvancementSyncStateService` + `Impl` | Holds the hash of the last successfully synced snapshot per player; skip-if-unchanged; cleared on untrack |

The snapshot hash is computed over the already-filtered snapshot, with advancements sorted by
identifier and criteria sorted by name, so that a pure reordering in the source file does not
count as a change. It lives in memory only; after a plugin restart the first sync per player
always sends.
| `SurfStatsApiImpl` | Implements the four new API methods; `onPlayerQuit` also processes advancements before untracking |

### `surf-stats-paper`

| Component | Responsibility |
|---|---|
| `SurfStatsPlugin.initializeServices` | Initialises `AdvancementsFileService` with `getStatsDataPath().resolveSibling("advancements")` |
| `SurfStatsPlugin.onDisableAsync` | Final advancement sync for all tracked players |
| `SurfStatsPlugin.flushPlayerAdvancements` | Reflection helper mirroring `flushPlayerStats`: `getHandle().getAdvancements().save()` |
| `listener/WorldSaveListener` | Handles `WorldSaveEvent`, debounced; flushes and syncs advancements for all tracked players |
| `listener/PlayerStatsListener` | Unchanged call site — `surfStatsApiImpl.onPlayerQuit` now covers advancements too |

`stats/` and `advancements/` are siblings inside the world directory. The NMS bridge exposes
only `getStatsDataPath()`, so `resolveSibling` is used. If the directory does not exist, the
service logs a warning at startup, matching `StatsFileServiceImpl`.

### `surf-stats-microservice`

| Component | Responsibility |
|---|---|
| `db/tables/AdvancementsTable` | Dimension table |
| `db/tables/PlayerAdvancementsTable` | Header rows |
| `db/tables/PlayerAdvancementCriteriaTable` | Criterion detail rows |
| `db/StatsDatabaseService.saveAdvancementSnapshots` | Groups by `(player, server)` via the existing `saveGroupedBatches` helper; returns failed UUIDs |
| `db/StatsDatabaseService.saveAdvancementSnapshot` | Writes one snapshot in one transaction |
| `handler/AdvancementsPacketHandler` | Handles `SaveAdvancementsRequestPacket`, responds with failed UUIDs |
| `StatsMicroservice.onBootstrap` | Registers the new handler |

## Data flow

```
PlayerQuitEvent (+1s delay)   ┐
WorldSaveEvent (30s debounce) ├─► flush() ─► advancements/<uuid>.json ─► parse ─► drop recipes/
plugin shutdown               ┘                                                     │
                                                          hash == last synced? ─► skip
                                                                                    │ no
                                       RabbitMQ ◄── SaveAdvancementsRequestPacket ◄──┘
                                           │
                                           ▼  per (player, server), sequential, one transaction
                     advancements insert-ignore
                       → DELETE player_advancement_criteria WHERE player + server
                       → DELETE player_advancements        WHERE player + server
                       → INSERT player_advancements
                       → INSERT player_advancement_criteria
```

### Why replace instead of upsert

Advancements can disappear: `/advancement revoke`, a removed datapack, a reset world. An
upsert-only strategy would leave those rows behind forever and the table would misrepresent
the current state. Since no history is kept, deleting and re-inserting the snapshot for one
`(player, server)` inside a single transaction is both simpler than a `NOT IN` diff and
portable across dialects.

The cost of the full rewrite is bounded by the skip-if-unchanged guard: most players do not
earn an advancement between two world saves, so the vast majority of syncs never leave the
plugin.

### Trigger details

- **Quit** — reuses the existing 1 second delay in `PlayerStatsListener`, which gives
  Minecraft time to flush the file after disconnect.
- **World save** — `WorldSaveEvent` fires once per world, so a server with three worlds fires
  three times per autosave. The listener ignores events within 30 seconds of the last handled
  one.
- **Shutdown** — `onDisableAsync` performs a final sync so advancements earned since the last
  autosave are not lost.

Only players in `StatisticsManagerService.trackedPlayers` are synced, consistent with the
statistics path.

## Error handling

- **Missing or unparsable file** → `Result.failure`, logged, resulting snapshot is empty →
  **nothing is sent**. This is the critical guard: a read failure must never wipe the
  database. The microservice repeats the empty-snapshot check before writing.
- **Malformed single entry** (invalid advancement identifier, unparsable timestamp) → that
  entry or criterion is skipped and logged; the rest of the file is still synced. An
  unparsable timestamp yields `awardedAt = null` rather than dropping the criterion.
- **Failed send** → the snapshot hash is only stored on a successful response. UUIDs returned
  in `StatsUuidResponsePacket` have their hash invalidated so the next trigger retries.
- **Database failure** → isolated per `(player, server)` group by the existing
  `saveGroupedBatches` helper; other players are unaffected.

## Testing

`surf-stats-core-common` already has JUnit configured; `surf-stats-core-client` does not, so
the same test dependency block is added there.

Unit tests cover the parser and the derived values:

- `DataVersion` is ignored rather than treated as an advancement.
- `recipes/` advancements are filtered out; non-recipe advancements in other namespaces are
  kept.
- Both timestamp formats parse (`2024-05-15 16:30:56 +0000` and ISO-8601).
- An unparsable timestamp yields `awardedAt = null` and keeps the criterion.
- A malformed advancement identifier is skipped without failing the whole file.
- `completedAt` is `null` when `done` is false, and the maximum criterion timestamp when true.
- `criteriaDone` equals the number of awarded criteria.
- The snapshot hash is stable against reordering and changes when a criterion is added.

Database behaviour has no test harness in this repository (no R2DBC test setup) and is
verified manually against a running microservice.

## Assumptions

- **Timestamp format.** Vanilla writes `yyyy-MM-dd HH:mm:ss Z`. Whether current Minecraft
  versions emit ISO-8601 instead could not be verified from documentation, so the parser
  attempts the vanilla pattern first and falls back to ISO-8601, then to `null`.
- **Directory layout.** `advancements/` is a sibling of `stats/` inside the world directory,
  reached via `resolveSibling`, because the NMS bridge exposes no dedicated getter.
- **Reflection target.** `ServerPlayer#getAdvancements()` and `PlayerAdvancements#save()` are
  reachable under Paper's Mojang mappings, the same way `getStats().save()` already is.

## Deferred

- **`criteria_total`.** The total number of criteria per advancement is not in the player
  file. Obtaining it requires the Bukkit advancement registry, which would pull a Bukkit
  dependency into the otherwise platform-agnostic `core-client`, or a fourth table that can
  disagree between servers running different datapacks. `criteria_done` alone answers "how far
  along is this player". Can be added later without touching the existing columns.
- **Opt-out for advancements.** No opt-out dimension is introduced. Adding a global toggle
  later only requires an extra check before the send.

## Documentation

`README.md` is updated: the module description, the *How It Works* section, the database
schema table, and the API usage section gain the advancement path.
