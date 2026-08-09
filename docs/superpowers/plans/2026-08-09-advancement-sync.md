# Advancement Synchronisation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Synchronise Minecraft player advancements from `<world>/advancements/<uuid>.json` into the Surf Stats database, alongside the existing statistics path.

**Architecture:** The Paper plugin reads and parses the native advancement JSON file, drops recipe advancements, and ships the complete snapshot over RabbitMQ. The microservice replaces the stored snapshot for that `(player, server)` inside one transaction. No history is kept — the tables always hold current state. A per-player content hash in the plugin suppresses sends when nothing changed.

**Tech Stack:** Kotlin, Gradle (Kotlin DSL), kotlinx.serialization, kotlinx.coroutines, mccoroutine-folia, Paper API, Exposed R2DBC (shadowed under `dev.slne.surf.database.libs.…`), RabbitMQ via `surf-rabbitmq`, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-09-advancement-sync-design.md`

## Global Constraints

- Advancement identifiers are stored as `VARCHAR(255)`; criterion names as `VARCHAR(128)`; server names as `VARCHAR(128)`.
- Advancement identifiers use the `Key` transform (`varchar(...).transform({ key(it) }, { it.asString() })`); **criterion names are plain `String`** — they are free-form and `Key.key()` throws on non-conforming input.
- Recipe advancements are filtered client-side: an advancement is dropped when its key **value** starts with `recipes/`, regardless of namespace.
- No history table, no opt-out handling, and no `criteria_total` column.
- Database tables are created **manually by the operator** — do not add schema-creation code. The DDL lives in the spec.
- Exposed imports come from the shadowed package `dev.slne.surf.database.libs.org.jetbrains.exposed.v1.…`.
- New files follow the existing pattern: an `interface` plus an `object …Impl`, with `companion object : Interface by Impl` for static delegation.
- Timestamps in the file are parsed with `yyyy-MM-dd HH:mm:ss Z` first, then ISO-8601; on failure the criterion is kept with `awardedAt = null`.
- Build verification command: `./gradlew build`.

---

## File Structure

**`surf-stats-api`** — serializable models shared by plugin and microservice

- Create `src/main/kotlin/dev/slne/surf/stats/api/model/AdvancementCriterion.kt` — one awarded criterion
- Create `src/main/kotlin/dev/slne/surf/stats/api/model/AdvancementEntry.kt` — one advancement plus derived `completedAt` / `criteriaDone`
- Create `src/main/kotlin/dev/slne/surf/stats/api/model/PlayerAdvancements.kt` — one player's snapshot on one server
- Modify `src/main/kotlin/dev/slne/surf/stats/api/SurfStatsApi.kt` — four new methods

**`surf-stats-core/surf-stats-core-common`** — RabbitMQ packet

- Create `src/main/kotlin/dev/slne/surf/stats/core/common/packets/SaveAdvancementsRequestPacket.kt`

**`surf-stats-core/surf-stats-core-client`** — reading, parsing, change detection, sending

- Create `src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementTimestamp.kt` — timestamp parsing only
- Create `src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsJsonModel.kt` — JSON → `List<AdvancementEntry>`, filtering, malformed-entry tolerance
- Create `src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsFileService.kt` + `AdvancementsFileServiceImpl.kt` — locating and reading files
- Create `src/main/kotlin/dev/slne/surf/stats/core/client/repository/PlayerAdvancementsRepository.kt` + `PlayerAdvancementsRepositoryImpl.kt` — adds player/server identity, turns failures into empty snapshots
- Create `src/main/kotlin/dev/slne/surf/stats/core/client/service/AdvancementSyncStateService.kt` + `AdvancementSyncStateServiceImpl.kt` — skip-if-unchanged
- Modify `src/main/kotlin/dev/slne/surf/stats/core/client/SurfStatsApiImpl.kt` — implement the new API methods
- Modify `build.gradle.kts` — add the JUnit test block

All new unit tests live in `surf-stats-core-client`'s test source set, including the tests for the `surf-stats-api` models. That module is where the whole read-and-parse pipeline lives and it sees the API models transitively, so one test setup covers everything instead of two.

**`surf-stats-paper`** — trigger points

- Create `src/main/kotlin/dev/slne/surf/stats/paper/listener/WorldSaveListener.kt` — debounced `WorldSaveEvent`
- Modify `src/main/kotlin/dev/slne/surf/stats/paper/SurfStatsPlugin.kt` — file service init, generalised reflection flush helper, shutdown sync, listener registration

**`surf-stats-microservice`** — persistence

- Create `src/main/kotlin/dev/slne/surf/stats/microservice/db/tables/AdvancementsTable.kt`
- Create `src/main/kotlin/dev/slne/surf/stats/microservice/db/tables/PlayerAdvancementsTable.kt`
- Create `src/main/kotlin/dev/slne/surf/stats/microservice/db/tables/PlayerAdvancementCriteriaTable.kt`
- Create `src/main/kotlin/dev/slne/surf/stats/microservice/db/GroupedSaver.kt` — extracted from `StatsDatabaseService`, shared by both domains
- Create `src/main/kotlin/dev/slne/surf/stats/microservice/db/AdvancementsDatabaseService.kt`
- Create `src/main/kotlin/dev/slne/surf/stats/microservice/handler/AdvancementsPacketHandler.kt`
- Modify `src/main/kotlin/dev/slne/surf/stats/microservice/db/StatsDatabaseService.kt` — use the extracted helper
- Modify `src/main/kotlin/dev/slne/surf/stats/microservice/StatsMicroservice.kt` — register the handler

**Deviation from the spec:** the spec placed `saveAdvancementSnapshots` on `StatsDatabaseService`. That file is already 341 lines covering opt-outs, servers and statistics; adding the advancement logic would push it past 430 lines across four domains. This plan puts it in its own `AdvancementsDatabaseService` and extracts the shared grouping helper instead. Same behaviour, better boundaries.

---

### Task 1: Advancement API models

**Files:**
- Create: `surf-stats-api/src/main/kotlin/dev/slne/surf/stats/api/model/AdvancementCriterion.kt`
- Create: `surf-stats-api/src/main/kotlin/dev/slne/surf/stats/api/model/AdvancementEntry.kt`
- Create: `surf-stats-api/src/main/kotlin/dev/slne/surf/stats/api/model/PlayerAdvancements.kt`
- Modify: `surf-stats-core/surf-stats-core-client/build.gradle.kts`
- Test: `surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/model/AdvancementEntryTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `AdvancementCriterion(name: String, awardedAt: Instant?)`, `AdvancementEntry(advancement: Key, done: Boolean, criteria: List<AdvancementCriterion>)` with computed `completedAt: Instant?` and `criteriaDone: Int`, `PlayerAdvancements(playerUuid: UUID, serverName: String, advancements: List<AdvancementEntry>) : Collection<AdvancementEntry>`. Every later task uses these.

- [ ] **Step 1: Add the test block to the core-client build file**

The module has no test configuration yet. Append to `surf-stats-core/surf-stats-core-client/build.gradle.kts`, extending the existing `dependencies` block and adding `tasks.test` at the end of the file:

```kotlin
dependencies {
    api(projects.surfStatsCore.surfStatsCoreCommon)
    compileOnlyApi(libs.surf.clan.api)

    // The `dev.slne.surf.api.gradle.core` convention plugin puts surf-api-core on
    // `compileOnly` only, so it never reaches the test classpath. Tests need it for
    // `key()` and, at runtime, for `logger()`. Same coordinate and version spec the
    // convention plugin itself uses.
    testImplementation("dev.slne.surf.api:surf-api-core:+")

    testImplementation("com.google.flogger:flogger:0.9")
    testRuntimeOnly("com.google.flogger:flogger-system-backend:0.9")

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("net.kyori:adventure-api:4.26.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

This mirrors `surf-stats-core/surf-stats-core-common/build.gradle.kts`, plus the explicit
`surf-api-core` line. That module declares a test block but has no test sources, so its
block was never exercised and does not cover the `dev.slne.surf.api.core.*` helpers.
`flogger` is needed because `logger()` from `surf-api-core` is used by the classes under
test in later tasks.

- [ ] **Step 2: Write the failing test**

Create `surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/model/AdvancementEntryTest.kt`:

```kotlin
package dev.slne.surf.stats.core.client.model

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.stats.api.model.AdvancementCriterion
import dev.slne.surf.stats.api.model.AdvancementEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class AdvancementEntryTest {

    private val root = key("minecraft:story/root")

    @Test
    fun `completedAt is the latest criterion timestamp when done`() {
        val entry = AdvancementEntry(
            advancement = root,
            done = true,
            criteria = listOf(
                AdvancementCriterion("a", Instant.parse("2024-05-15T16:00:00Z")),
                AdvancementCriterion("b", Instant.parse("2024-05-16T16:00:00Z")),
            )
        )

        assertEquals(Instant.parse("2024-05-16T16:00:00Z"), entry.completedAt)
    }

    @Test
    fun `completedAt is null when the advancement is not done`() {
        val entry = AdvancementEntry(
            advancement = root,
            done = false,
            criteria = listOf(AdvancementCriterion("a", Instant.parse("2024-05-15T16:00:00Z")))
        )

        assertNull(entry.completedAt)
    }

    @Test
    fun `completedAt is null when done but no timestamp parsed`() {
        val entry = AdvancementEntry(
            advancement = root,
            done = true,
            criteria = listOf(AdvancementCriterion("a", null))
        )

        assertNull(entry.completedAt)
    }

    @Test
    fun `criteriaDone counts every awarded criterion including undated ones`() {
        val entry = AdvancementEntry(
            advancement = root,
            done = false,
            criteria = listOf(
                AdvancementCriterion("a", Instant.parse("2024-05-15T16:00:00Z")),
                AdvancementCriterion("b", null),
            )
        )

        assertEquals(2, entry.criteriaDone)
    }

    @Test
    fun `criteriaDone is zero without criteria`() {
        assertEquals(0, AdvancementEntry(advancement = root, done = false).criteriaDone)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementEntryTest*"`
Expected: FAIL — compilation error, `AdvancementCriterion` and `AdvancementEntry` are unresolved.

- [ ] **Step 4: Create `AdvancementCriterion`**

```kotlin
package dev.slne.surf.stats.api.model

import dev.slne.surf.api.core.serializer.java.datetime.datetime.instant.SerializableInstant
import kotlinx.serialization.Serializable

/**
 * A single criterion of an advancement that the player has been awarded.
 *
 * Criterion names are free-form strings chosen by the advancement author
 * (`has_log`, `in_bed`, `minecraft:plains`) and are deliberately **not**
 * modelled as a [net.kyori.adventure.key.Key] — datapacks are not required to
 * use valid resource locations.
 *
 * @property awardedAt when the criterion was awarded, or `null` when the
 *   timestamp in the source file could not be parsed. The criterion still
 *   counts as awarded in that case.
 */
@Serializable
data class AdvancementCriterion(
    val name: String,
    val awardedAt: SerializableInstant? = null
)
```

- [ ] **Step 5: Create `AdvancementEntry`**

```kotlin
package dev.slne.surf.stats.api.model

import dev.slne.surf.api.core.serializer.adventure.key.SerializableKey
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * A player's progress on one advancement.
 *
 * [completedAt] and [criteriaDone] are computed properties without backing
 * fields, so kotlinx.serialization does not put them on the wire — the
 * microservice derives them when writing.
 */
@Serializable
data class AdvancementEntry(
    val advancement: SerializableKey,
    val done: Boolean,
    val criteria: List<AdvancementCriterion> = emptyList()
) {
    /** The latest criterion timestamp once the advancement is complete, else `null`. */
    val completedAt: Instant?
        get() = if (done) criteria.mapNotNull { it.awardedAt }.maxOrNull() else null

    /** Number of awarded criteria. The total is not present in the source file. */
    val criteriaDone: Int get() = criteria.size
}
```

- [ ] **Step 6: Create `PlayerAdvancements`**

```kotlin
package dev.slne.surf.stats.api.model

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable

/**
 * One player's complete advancement snapshot on one server.
 *
 * Snapshots are always complete: the microservice replaces everything it has
 * stored for `(playerUuid, serverName)` with this content. An empty snapshot is
 * therefore never sent — see `PlayerAdvancementsRepository`.
 */
@Serializable
data class PlayerAdvancements(
    val playerUuid: SerializableUUID,
    val serverName: String,
    val advancements: List<AdvancementEntry> = emptyList()
) : Collection<AdvancementEntry> {
    override fun contains(element: AdvancementEntry): Boolean = advancements.contains(element)

    override fun containsAll(elements: Collection<AdvancementEntry>): Boolean =
        advancements.containsAll(elements)

    override fun isEmpty(): Boolean = advancements.isEmpty()

    override fun iterator(): Iterator<AdvancementEntry> = advancements.iterator()

    override val size: Int get() = advancements.size
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementEntryTest*"`
Expected: PASS, 5 tests.

- [ ] **Step 8: Commit**

```bash
git add surf-stats-api/src/main/kotlin/dev/slne/surf/stats/api/model/ \
        surf-stats-core/surf-stats-core-client/build.gradle.kts \
        surf-stats-core/surf-stats-core-client/src/test/
git commit -m "✨ feat(api): add advancement snapshot models"
```

---

### Task 2: Advancement timestamp parsing

**Files:**
- Create: `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementTimestamp.kt`
- Test: `surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/json/AdvancementTimestampTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `AdvancementTimestamp.parse(raw: String): Instant?` — used by Task 3.

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.slne.surf.stats.core.client.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class AdvancementTimestampTest {

    @Test
    fun `parses the vanilla format`() {
        assertEquals(
            Instant.parse("2024-05-15T16:30:56Z"),
            AdvancementTimestamp.parse("2024-05-15 16:30:56 +0000")
        )
    }

    @Test
    fun `applies the offset of the vanilla format`() {
        assertEquals(
            Instant.parse("2024-05-15T14:30:56Z"),
            AdvancementTimestamp.parse("2024-05-15 16:30:56 +0200")
        )
    }

    @Test
    fun `parses an ISO instant`() {
        assertEquals(
            Instant.parse("2024-05-15T16:30:56Z"),
            AdvancementTimestamp.parse("2024-05-15T16:30:56Z")
        )
    }

    @Test
    fun `parses an ISO offset date time`() {
        assertEquals(
            Instant.parse("2024-05-15T14:30:56Z"),
            AdvancementTimestamp.parse("2024-05-15T16:30:56+02:00")
        )
    }

    @Test
    fun `returns null for an unparsable value`() {
        assertNull(AdvancementTimestamp.parse("not a timestamp"))
    }

    @Test
    fun `returns null for an empty value`() {
        assertNull(AdvancementTimestamp.parse(""))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementTimestampTest*"`
Expected: FAIL — compilation error, `AdvancementTimestamp` is unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package dev.slne.surf.stats.core.client.json

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Parses the criterion timestamps written into `advancements/<uuid>.json`.
 *
 * Vanilla writes `yyyy-MM-dd HH:mm:ss Z` (for example `2024-05-15 16:30:56 +0000`).
 * It could not be verified from the documentation whether newer versions emit
 * ISO-8601 instead, so both are accepted. An unparsable value yields `null`
 * rather than dropping the criterion — the fact that it was awarded still matters.
 */
object AdvancementTimestamp {
    private val VANILLA_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT)

    fun parse(raw: String): Instant? =
        tryParse(raw) { OffsetDateTime.parse(it, VANILLA_FORMAT).toInstant() }
            ?: tryParse(raw) { Instant.parse(it) }
            ?: tryParse(raw) { OffsetDateTime.parse(it).toInstant() }

    private inline fun tryParse(raw: String, parse: (String) -> Instant): Instant? =
        runCatching { parse(raw) }.getOrNull()
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementTimestampTest*"`
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementTimestamp.kt \
        surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/json/AdvancementTimestampTest.kt
git commit -m "✨ feat(client): parse advancement criterion timestamps"
```

---

### Task 3: Advancement JSON parser

**Files:**
- Create: `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsJsonModel.kt`
- Test: `surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsJsonModelTest.kt`

**Interfaces:**
- Consumes: `AdvancementEntry`, `AdvancementCriterion` (Task 1); `AdvancementTimestamp.parse` (Task 2).
- Produces: `AdvancementsJsonModel.parse(content: String): List<AdvancementEntry>` — used by Task 4.

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.slne.surf.stats.core.client.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AdvancementsJsonModelTest {

    @Test
    fun `ignores the DataVersion key`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "minecraft:story/root": { "criteria": { "crafting_table": "2024-05-15 16:04:02 +0000" }, "done": true },
              "DataVersion": 4189
            }
            """.trimIndent()
        )

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `filters recipe advancements in any namespace`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "minecraft:recipes/misc/charcoal": { "criteria": { "has_log": "2024-05-15 16:04:02 +0000" }, "done": true },
              "mypack:recipes/custom/thing": { "criteria": {}, "done": false },
              "minecraft:story/root": { "criteria": {}, "done": true }
            }
            """.trimIndent()
        )

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `keeps non-recipe advancements from foreign namespaces`() {
        val entries = AdvancementsJsonModel.parse(
            """
            { "mypack:custom/thing": { "criteria": {}, "done": true } }
            """.trimIndent()
        )

        assertEquals(listOf("mypack:custom/thing"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `skips malformed advancement identifiers`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "Not A Valid Key": { "criteria": {}, "done": true },
              "minecraft:story/root": { "criteria": {}, "done": true }
            }
            """.trimIndent()
        )

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `skips entries whose body is not an advancement object`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "minecraft:story/broken": "unexpected",
              "minecraft:story/root": { "criteria": {}, "done": true }
            }
            """.trimIndent()
        )

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `maps criteria with their timestamps`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "minecraft:adventure/adventuring_time": {
                "criteria": {
                  "minecraft:plains": "2024-05-15 16:30:56 +0000",
                  "minecraft:desert": "2024-05-16 09:02:11 +0000"
                },
                "done": false
              }
            }
            """.trimIndent()
        )

        val entry = entries.single()
        assertFalse(entry.done)
        assertEquals(2, entry.criteriaDone)
        assertNull(entry.completedAt)
        assertEquals(
            Instant.parse("2024-05-15T16:30:56Z"),
            entry.criteria.single { it.name == "minecraft:plains" }.awardedAt
        )
    }

    @Test
    fun `keeps criteria whose timestamp cannot be parsed`() {
        val entries = AdvancementsJsonModel.parse(
            """
            { "minecraft:story/root": { "criteria": { "crafting_table": "garbage" }, "done": true } }
            """.trimIndent()
        )

        val criterion = entries.single().criteria.single()
        assertEquals("crafting_table", criterion.name)
        assertNull(criterion.awardedAt)
    }

    @Test
    fun `returns an empty list for an empty object`() {
        assertTrue(AdvancementsJsonModel.parse("{}").isEmpty())
    }

    @Test
    fun `returns an empty list when only DataVersion is present`() {
        assertTrue(AdvancementsJsonModel.parse("""{ "DataVersion": 4189 }""").isEmpty())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementsJsonModelTest*"`
Expected: FAIL — compilation error, `AdvancementsJsonModel` is unresolved.

- [ ] **Step 3: Write the implementation**

The top level of the file is heterogeneous — advancement objects plus a `DataVersion` integer — so it cannot be a single `@Serializable` data class the way `StatsJsonModel` is. It is decoded as a `JsonObject` and each value is decoded individually.

```kotlin
package dev.slne.surf.stats.core.client.json

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.stats.api.model.AdvancementCriterion
import dev.slne.surf.stats.api.model.AdvancementEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Parses the Minecraft player advancement file.
 * File location: `<world>/advancements/<uuid>.json`
 *
 * Example structure:
 * ```json
 * {
 *   "minecraft:adventure/adventuring_time": {
 *     "criteria": { "minecraft:plains": "2024-05-15 16:30:56 +0000" },
 *     "done": false
 *   },
 *   "DataVersion": 4189
 * }
 * ```
 *
 * Recipe advancements are dropped: they are a gameplay mechanic rather than a
 * statistic and outnumber real advancements roughly ten to one. Individual
 * malformed entries are skipped so that one bad entry cannot cost a player
 * their entire snapshot.
 */
object AdvancementsJsonModel {
    private val log = logger()

    private const val DATA_VERSION_KEY = "DataVersion"
    private const val RECIPE_PATH_PREFIX = "recipes/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(content: String): List<AdvancementEntry> {
        val root = json.parseToJsonElement(content).jsonObject

        return root.mapNotNull { (rawId, element) ->
            if (rawId == DATA_VERSION_KEY) return@mapNotNull null

            val advancement = runCatching { key(rawId) }.getOrElse {
                log.atWarning().log("Skipping advancement with invalid identifier: %s", rawId)
                return@mapNotNull null
            }

            if (advancement.value().startsWith(RECIPE_PATH_PREFIX)) return@mapNotNull null

            val progress = runCatching { json.decodeFromJsonElement<RawProgress>(element) }
                .getOrElse { error ->
                    log.atWarning()
                        .withCause(error)
                        .log("Skipping malformed advancement entry: %s", rawId)
                    return@mapNotNull null
                }

            AdvancementEntry(
                advancement = advancement,
                done = progress.done,
                criteria = progress.criteria.map { (name, rawTimestamp) ->
                    AdvancementCriterion(
                        name = name,
                        awardedAt = AdvancementTimestamp.parse(rawTimestamp)
                    )
                }
            )
        }
    }

    @Serializable
    private data class RawProgress(
        val criteria: Map<String, String> = emptyMap(),
        val done: Boolean = false
    )
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementsJsonModelTest*"`
Expected: PASS, 9 tests.

- [ ] **Step 5: Commit**

```bash
git add surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsJsonModel.kt \
        surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsJsonModelTest.kt
git commit -m "✨ feat(client): parse advancement progress files"
```

---

### Task 4: Advancement file service

**Files:**
- Create: `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsFileService.kt`
- Create: `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsFileServiceImpl.kt`
- Test: `surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsFileServiceTest.kt`

**Interfaces:**
- Consumes: `AdvancementsJsonModel.parse` (Task 3).
- Produces: `AdvancementsFileService` with `initialize(advancementsDirectory: Path)`, `loadAdvancements(playerUuid: UUID): Result<List<AdvancementEntry>>`, `loadAllAdvancements(playerUuids: Set<UUID>): Map<UUID, Result<List<AdvancementEntry>>>`, `getAdvancementsFilePath(playerUuid: UUID): Path`, `advancementsExist(playerUuid: UUID): Boolean`, plus `companion object : AdvancementsFileService by AdvancementsFileServiceImpl`. Used by Tasks 6 and 10.

Note it returns `List<AdvancementEntry>` rather than `PlayerAdvancements`. `StatsFileServiceImpl` calls `SurfCoreApi.getCurrentServerName()` internally, which makes it untestable outside a running server. Keeping server identity in the repository layer (Task 6) leaves this service testable and gives it a single responsibility: files.

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.slne.surf.stats.core.client.json

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import kotlin.io.path.writeText

class AdvancementsFileServiceTest {

    private val player: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private fun writeAdvancements(directory: Path, uuid: UUID, content: String) {
        directory.resolve("$uuid.json").writeText(content)
    }

    @Test
    fun `loads advancements from disk`(@TempDir directory: Path) = runTest {
        AdvancementsFileService.initialize(directory)
        writeAdvancements(
            directory,
            player,
            """{ "minecraft:story/root": { "criteria": {}, "done": true } }"""
        )

        val entries = AdvancementsFileService.loadAdvancements(player).getOrThrow()

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `returns an empty success when the file does not exist`(@TempDir directory: Path) = runTest {
        AdvancementsFileService.initialize(directory)

        val result = AdvancementsFileService.loadAdvancements(player)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `returns a failure for unreadable json`(@TempDir directory: Path) = runTest {
        AdvancementsFileService.initialize(directory)
        writeAdvancements(directory, player, "this is not json")

        assertTrue(AdvancementsFileService.loadAdvancements(player).isFailure)
    }

    @Test
    fun `loads several players and keeps failures separate`(@TempDir directory: Path) = runTest {
        val broken = UUID.fromString("00000000-0000-0000-0000-000000000002")
        AdvancementsFileService.initialize(directory)
        writeAdvancements(
            directory,
            player,
            """{ "minecraft:story/root": { "criteria": {}, "done": true } }"""
        )
        writeAdvancements(directory, broken, "this is not json")

        val results = AdvancementsFileService.loadAllAdvancements(setOf(player, broken))

        assertEquals(1, results.getValue(player).getOrThrow().size)
        assertTrue(results.getValue(broken).isFailure)
    }

    @Test
    fun `reports whether a file exists`(@TempDir directory: Path) = runTest {
        AdvancementsFileService.initialize(directory)

        assertFalse(AdvancementsFileService.advancementsExist(player))

        writeAdvancements(directory, player, "{}")

        assertTrue(AdvancementsFileService.advancementsExist(player))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementsFileServiceTest*"`
Expected: FAIL — compilation error, `AdvancementsFileService` is unresolved.

- [ ] **Step 3: Write the interface**

```kotlin
package dev.slne.surf.stats.core.client.json

import dev.slne.surf.stats.api.model.AdvancementEntry
import java.nio.file.Path
import java.util.*

/**
 * Reads player advancement files from `<world>/advancements/`.
 *
 * Returns bare entry lists rather than [dev.slne.surf.stats.api.model.PlayerAdvancements];
 * attaching the player and server identity is the repository's job.
 */
interface AdvancementsFileService {
    /**
     * Initializes the service with the base advancements directory.
     *
     * @param advancementsDirectory the directory containing player advancement files
     */
    suspend fun initialize(advancementsDirectory: Path)

    /**
     * Reads and parses a player's advancement file.
     *
     * @return a successful [Result] with an empty list when the file does not
     *   exist, or a failure when it exists but cannot be read or parsed
     */
    suspend fun loadAdvancements(playerUuid: UUID): Result<List<AdvancementEntry>>

    /**
     * Reads advancements for multiple players, allowing partial success.
     */
    suspend fun loadAllAdvancements(playerUuids: Set<UUID>): Map<UUID, Result<List<AdvancementEntry>>>

    /**
     * Gets the file path for a player's advancement file.
     */
    fun getAdvancementsFilePath(playerUuid: UUID): Path

    /**
     * Checks whether an advancement file exists for the given player.
     */
    suspend fun advancementsExist(playerUuid: UUID): Boolean

    companion object : AdvancementsFileService by AdvancementsFileServiceImpl
}
```

- [ ] **Step 4: Write the implementation**

```kotlin
package dev.slne.surf.stats.core.client.json

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.stats.api.model.AdvancementEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Implementation of [AdvancementsFileService] that reads the native Minecraft
 * advancement JSON files.
 */
object AdvancementsFileServiceImpl : AdvancementsFileService {
    private val log = logger()

    private lateinit var advancementsDirectory: Path

    override suspend fun initialize(advancementsDirectory: Path) {
        this.advancementsDirectory = advancementsDirectory
        withContext(Dispatchers.IO) {
            if (!Files.exists(advancementsDirectory)) {
                log.atWarning()
                    .log("Advancements directory does not exist: $advancementsDirectory")
            } else if (!Files.isDirectory(advancementsDirectory)) {
                throw IllegalArgumentException(
                    "Advancements path is not a directory: $advancementsDirectory"
                )
            }
        }
    }

    override suspend fun loadAdvancements(playerUuid: UUID): Result<List<AdvancementEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val filePath = getAdvancementsFilePath(playerUuid)

                if (!filePath.exists()) {
                    return@runCatching emptyList<AdvancementEntry>()
                }

                AdvancementsJsonModel.parse(filePath.readText())
            }.onFailure { error ->
                log.atSevere()
                    .withCause(error)
                    .log("Failed to load advancements for player $playerUuid")
            }
        }

    override suspend fun loadAllAdvancements(
        playerUuids: Set<UUID>
    ): Map<UUID, Result<List<AdvancementEntry>>> = coroutineScope {
        playerUuids.map { uuid ->
            async { uuid to loadAdvancements(uuid) }
        }.awaitAll().toMap()
    }

    override fun getAdvancementsFilePath(playerUuid: UUID): Path =
        advancementsDirectory.resolve("$playerUuid.json")

    override suspend fun advancementsExist(playerUuid: UUID): Boolean =
        withContext(Dispatchers.IO) { getAdvancementsFilePath(playerUuid).exists() }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementsFileServiceTest*"`
Expected: PASS, 5 tests.

- [ ] **Step 6: Commit**

```bash
git add surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsFileService.kt \
        surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsFileServiceImpl.kt \
        surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/json/AdvancementsFileServiceTest.kt
git commit -m "✨ feat(client): read player advancement files"
```

---

### Task 5: Advancement sync state service

**Files:**
- Create: `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/service/AdvancementSyncStateService.kt`
- Create: `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/service/AdvancementSyncStateServiceImpl.kt`
- Test: `surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/service/AdvancementSyncStateServiceTest.kt`

**Interfaces:**
- Consumes: `PlayerAdvancements` (Task 1).
- Produces: `AdvancementSyncStateService` with `selectChanged(snapshots: List<PlayerAdvancements>): List<PlayerAdvancements>`, `markSynced(sent: List<PlayerAdvancements>, failed: Set<UUID>)`, `forget(playerUuid: UUID)`, plus `companion object : AdvancementSyncStateService by AdvancementSyncStateServiceImpl`. Used by Task 6.

This is where the skip-if-unchanged decision lives, deliberately separated from the RabbitMQ call so it can be unit tested — `SurfStatsApiImpl` is then pure wiring.

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.slne.surf.stats.core.client.service

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.stats.api.model.AdvancementCriterion
import dev.slne.surf.stats.api.model.AdvancementEntry
import dev.slne.surf.stats.api.model.PlayerAdvancements
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class AdvancementSyncStateServiceTest {

    private val player: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val other: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private val awardedAt = Instant.parse("2024-05-15T16:00:00Z")

    @BeforeEach
    fun resetState() {
        AdvancementSyncStateService.forget(player)
        AdvancementSyncStateService.forget(other)
    }

    private fun snapshot(
        uuid: UUID = player,
        serverName: String = "survival",
        advancements: List<AdvancementEntry>
    ) = PlayerAdvancements(uuid, serverName, advancements)

    private fun root(done: Boolean = true, criteria: List<AdvancementCriterion> = emptyList()) =
        AdvancementEntry(key("minecraft:story/root"), done, criteria)

    private fun mineDiamond() = AdvancementEntry(key("minecraft:story/mine_diamond"), true)

    @Test
    fun `selects every snapshot on the first sync`() {
        val snapshots = listOf(snapshot(advancements = listOf(root())))

        assertEquals(snapshots, AdvancementSyncStateService.selectChanged(snapshots))
    }

    @Test
    fun `skips an unchanged snapshot after it was marked synced`() {
        val snapshots = listOf(snapshot(advancements = listOf(root())))
        AdvancementSyncStateService.markSynced(snapshots, failed = emptySet())

        assertTrue(AdvancementSyncStateService.selectChanged(snapshots).isEmpty())
    }

    @Test
    fun `ignores ordering differences`() {
        val ordered = listOf(snapshot(advancements = listOf(root(), mineDiamond())))
        AdvancementSyncStateService.markSynced(ordered, failed = emptySet())

        val reordered = listOf(snapshot(advancements = listOf(mineDiamond(), root())))

        assertTrue(AdvancementSyncStateService.selectChanged(reordered).isEmpty())
    }

    @Test
    fun `ignores criterion ordering differences`() {
        val ordered = listOf(
            snapshot(
                advancements = listOf(
                    root(
                        done = false,
                        criteria = listOf(
                            AdvancementCriterion("a", awardedAt),
                            AdvancementCriterion("b", awardedAt),
                        )
                    )
                )
            )
        )
        AdvancementSyncStateService.markSynced(ordered, failed = emptySet())

        val reordered = listOf(
            snapshot(
                advancements = listOf(
                    root(
                        done = false,
                        criteria = listOf(
                            AdvancementCriterion("b", awardedAt),
                            AdvancementCriterion("a", awardedAt),
                        )
                    )
                )
            )
        )

        assertTrue(AdvancementSyncStateService.selectChanged(reordered).isEmpty())
    }

    @Test
    fun `detects an added advancement`() {
        AdvancementSyncStateService.markSynced(
            listOf(snapshot(advancements = listOf(root()))),
            failed = emptySet()
        )

        val grown = listOf(snapshot(advancements = listOf(root(), mineDiamond())))

        assertEquals(grown, AdvancementSyncStateService.selectChanged(grown))
    }

    @Test
    fun `detects an added criterion`() {
        AdvancementSyncStateService.markSynced(
            listOf(
                snapshot(
                    advancements = listOf(
                        root(done = false, criteria = listOf(AdvancementCriterion("a", awardedAt)))
                    )
                )
            ),
            failed = emptySet()
        )

        val grown = listOf(
            snapshot(
                advancements = listOf(
                    root(
                        done = false,
                        criteria = listOf(
                            AdvancementCriterion("a", awardedAt),
                            AdvancementCriterion("b", awardedAt),
                        )
                    )
                )
            )
        )

        assertEquals(grown, AdvancementSyncStateService.selectChanged(grown))
    }

    @Test
    fun `detects a removed advancement`() {
        AdvancementSyncStateService.markSynced(
            listOf(snapshot(advancements = listOf(root(), mineDiamond()))),
            failed = emptySet()
        )

        val shrunk = listOf(snapshot(advancements = listOf(root())))

        assertEquals(shrunk, AdvancementSyncStateService.selectChanged(shrunk))
    }

    @Test
    fun `detects a changed done flag`() {
        AdvancementSyncStateService.markSynced(
            listOf(snapshot(advancements = listOf(root(done = false)))),
            failed = emptySet()
        )

        val completed = listOf(snapshot(advancements = listOf(root(done = true))))

        assertEquals(completed, AdvancementSyncStateService.selectChanged(completed))
    }

    @Test
    fun `does not mark failed players as synced`() {
        val snapshots = listOf(
            snapshot(uuid = player, advancements = listOf(root())),
            snapshot(uuid = other, advancements = listOf(root())),
        )
        AdvancementSyncStateService.markSynced(snapshots, failed = setOf(player))

        assertEquals(
            listOf(player),
            AdvancementSyncStateService.selectChanged(snapshots).map { it.playerUuid }
        )
    }

    @Test
    fun `forget makes the next snapshot count as changed`() {
        val snapshots = listOf(snapshot(advancements = listOf(root())))
        AdvancementSyncStateService.markSynced(snapshots, failed = emptySet())

        AdvancementSyncStateService.forget(player)

        assertEquals(snapshots, AdvancementSyncStateService.selectChanged(snapshots))
    }

    @Test
    fun `detects a changed server name`() {
        AdvancementSyncStateService.markSynced(
            listOf(snapshot(serverName = "survival", advancements = listOf(root()))),
            failed = emptySet()
        )

        val otherServer = listOf(snapshot(serverName = "creative", advancements = listOf(root())))

        assertEquals(otherServer, AdvancementSyncStateService.selectChanged(otherServer))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementSyncStateServiceTest*"`
Expected: FAIL — compilation error, `AdvancementSyncStateService` is unresolved.

- [ ] **Step 3: Write the interface**

```kotlin
package dev.slne.surf.stats.core.client.service

import dev.slne.surf.stats.api.model.PlayerAdvancements
import java.util.*

/**
 * Suppresses advancement sends for players whose snapshot has not changed since
 * the last successful send.
 *
 * Advancement snapshots are shipped in full and most players earn nothing
 * between two world saves, so without this guard the majority of sends would
 * rewrite identical rows.
 *
 * State is in-memory only — after a plugin restart the first snapshot per player
 * always counts as changed.
 */
interface AdvancementSyncStateService {
    /**
     * Returns the snapshots whose content differs from the last state marked
     * synced for that player.
     */
    fun selectChanged(snapshots: List<PlayerAdvancements>): List<PlayerAdvancements>

    /**
     * Records [sent] as the last successfully synced state, skipping players
     * listed in [failed] so the next trigger retries them.
     */
    fun markSynced(sent: List<PlayerAdvancements>, failed: Set<UUID>)

    /**
     * Drops all state for [playerUuid], for example when the player quits.
     */
    fun forget(playerUuid: UUID)

    companion object : AdvancementSyncStateService by AdvancementSyncStateServiceImpl
}
```

- [ ] **Step 4: Write the implementation**

The hash is computed over the snapshot with advancements sorted by identifier and criteria sorted by name, so a pure reordering in the source file is not treated as a change. A collision only ever matters against that same player's previous snapshot, so a 32-bit hash is sufficient.

```kotlin
package dev.slne.surf.stats.core.client.service

import dev.slne.surf.stats.api.model.PlayerAdvancements
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AdvancementSyncStateServiceImpl : AdvancementSyncStateService {
    private val lastSyncedHashes = ConcurrentHashMap<UUID, Int>()

    override fun selectChanged(snapshots: List<PlayerAdvancements>): List<PlayerAdvancements> =
        snapshots.filter { snapshot ->
            lastSyncedHashes[snapshot.playerUuid] != hashOf(snapshot)
        }

    override fun markSynced(sent: List<PlayerAdvancements>, failed: Set<UUID>) {
        sent.forEach { snapshot ->
            if (snapshot.playerUuid in failed) {
                lastSyncedHashes.remove(snapshot.playerUuid)
            } else {
                lastSyncedHashes[snapshot.playerUuid] = hashOf(snapshot)
            }
        }
    }

    override fun forget(playerUuid: UUID) {
        lastSyncedHashes.remove(playerUuid)
    }

    /**
     * Order-independent content hash of [snapshot].
     */
    private fun hashOf(snapshot: PlayerAdvancements): Int {
        var result = snapshot.serverName.hashCode()

        snapshot.advancements
            .sortedBy { it.advancement.asString() }
            .forEach { entry ->
                result = 31 * result + entry.advancement.asString().hashCode()
                result = 31 * result + entry.done.hashCode()

                entry.criteria
                    .sortedBy { it.name }
                    .forEach { criterion ->
                        result = 31 * result + criterion.name.hashCode()
                        result = 31 * result + (criterion.awardedAt?.hashCode() ?: 0)
                    }
            }

        return result
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:test --tests "*AdvancementSyncStateServiceTest*"`
Expected: PASS, 11 tests.

- [ ] **Step 6: Commit**

```bash
git add surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/service/AdvancementSyncStateService.kt \
        surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/service/AdvancementSyncStateServiceImpl.kt \
        surf-stats-core/surf-stats-core-client/src/test/kotlin/dev/slne/surf/stats/core/client/service/AdvancementSyncStateServiceTest.kt
git commit -m "✨ feat(client): skip unchanged advancement snapshots"
```

---

### Task 6: Repository, packet and API wiring

**Files:**
- Create: `surf-stats-core/surf-stats-core-common/src/main/kotlin/dev/slne/surf/stats/core/common/packets/SaveAdvancementsRequestPacket.kt`
- Create: `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/repository/PlayerAdvancementsRepository.kt`
- Create: `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/repository/PlayerAdvancementsRepositoryImpl.kt`
- Modify: `surf-stats-api/src/main/kotlin/dev/slne/surf/stats/api/SurfStatsApi.kt`
- Modify: `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/SurfStatsApiImpl.kt`

**Interfaces:**
- Consumes: `AdvancementsFileService` (Task 4), `AdvancementSyncStateService` (Task 5), `PlayerAdvancements` (Task 1), existing `StatsUuidResponsePacket`.
- Produces: `SaveAdvancementsRequestPacket(players: List<PlayerAdvancements>)` — consumed by Task 9. `SurfStatsApi.processAllPlayerAdvancements(uuids: Set<UUID>)` and `processPlayerAdvancements(playerUuid: UUID)` — consumed by Task 10. `PlayerAdvancementsRepository.loadAllAdvancements(uuids: Set<UUID>): List<PlayerAdvancements>`.

This task has no unit tests. The repository is a thin adapter over the already-tested file service, and everything else is wiring against `statsInstance.rabbitApi`, which is resolved through a `ServiceLoader` and has no test harness in this repository. The decision logic that *could* go wrong lives in Task 5 and is tested there. Verification here is compilation plus the manual check in Task 11.

- [ ] **Step 1: Create the packet**

```kotlin
package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import dev.slne.surf.stats.api.model.PlayerAdvancements
import kotlinx.serialization.Serializable

/**
 * Ships complete advancement snapshots to the microservice.
 *
 * Each entry replaces everything stored for its `(playerUuid, serverName)`.
 * The response carries the UUIDs of players whose snapshot failed to persist.
 */
@Serializable
data class SaveAdvancementsRequestPacket(
    val players: List<PlayerAdvancements>
) : RabbitRequestPacket<StatsUuidResponsePacket>()
```

- [ ] **Step 2: Create the repository interface**

```kotlin
package dev.slne.surf.stats.core.client.repository

import dev.slne.surf.stats.api.model.PlayerAdvancements
import java.util.*

/**
 * Loads player advancement snapshots from the filesystem and attaches the
 * current server identity.
 */
interface PlayerAdvancementsRepository {
    /**
     * Loads one player's snapshot. Read failures yield an empty snapshot, which
     * callers must never send — see [loadAllAdvancements].
     */
    suspend fun loadAdvancements(uuid: UUID): PlayerAdvancements

    /**
     * Loads snapshots for multiple players, dropping the ones that failed to read.
     */
    suspend fun loadAllAdvancements(uuids: Set<UUID>): List<PlayerAdvancements>

    companion object : PlayerAdvancementsRepository by PlayerAdvancementsRepositoryImpl
}
```

- [ ] **Step 3: Create the repository implementation**

```kotlin
package dev.slne.surf.stats.core.client.repository

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.stats.api.model.AdvancementEntry
import dev.slne.surf.stats.api.model.PlayerAdvancements
import dev.slne.surf.stats.core.client.json.AdvancementsFileService
import java.util.*

object PlayerAdvancementsRepositoryImpl : PlayerAdvancementsRepository {
    private val log = logger()

    override suspend fun loadAdvancements(uuid: UUID): PlayerAdvancements =
        snapshot(uuid, AdvancementsFileService.loadAdvancements(uuid).getOrElse { emptyList() })

    override suspend fun loadAllAdvancements(uuids: Set<UUID>): List<PlayerAdvancements> {
        val results = AdvancementsFileService.loadAllAdvancements(uuids)

        val failed = results.filterValues { it.isFailure }.keys
        if (failed.isNotEmpty()) {
            log.atWarning()
                .log("Failed to load advancements for {} players: {}", failed.size, failed)
        }

        return results.mapNotNull { (uuid, result) ->
            result.getOrNull()?.let { entries -> snapshot(uuid, entries) }
        }
    }

    private fun snapshot(uuid: UUID, entries: List<AdvancementEntry>) = PlayerAdvancements(
        playerUuid = uuid,
        serverName = SurfCoreApi.getCurrentServerName(),
        advancements = entries
    )
}
```

Note the asymmetry with `loadAdvancements`: a single-player read failure collapses to an empty snapshot, which the API layer then refuses to send. `loadAllAdvancements` drops failed players entirely so a broken file for one player never affects the others.

- [ ] **Step 4: Add the API methods to `SurfStatsApi`**

Insert after the existing `saveDiffStats` declaration in `surf-stats-api/src/main/kotlin/dev/slne/surf/stats/api/SurfStatsApi.kt`, before the `companion object`:

```kotlin
    /**
     * Loads the player's current advancement snapshot from disk.
     *
     * Recipe advancements are excluded.
     */
    suspend fun getPlayerAdvancements(playerUuid: UUID): PlayerAdvancements

    /**
     * Replaces everything stored for `(playerUuid, serverName)` with [advancements].
     *
     * The snapshot must be complete — anything missing from it is deleted. An
     * empty snapshot is ignored rather than treated as "the player has nothing",
     * so that a failed read can never wipe stored data.
     *
     * This bypasses the change-detection used by [processPlayerAdvancements] and
     * always sends.
     */
    suspend fun saveAdvancements(playerUuid: UUID, advancements: PlayerAdvancements)

    /**
     * Loads one player's advancements from disk and sends them if they changed
     * since the last successful send.
     */
    suspend fun processPlayerAdvancements(playerUuid: UUID)

    /**
     * Same as [processPlayerAdvancements], batched for many players. Players whose
     * snapshot is unchanged are not sent at all.
     */
    suspend fun processAllPlayerAdvancements(uuids: Set<UUID>)
```

Add the import at the top of the file:

```kotlin
import dev.slne.surf.stats.api.model.PlayerAdvancements
```

- [ ] **Step 5: Implement the methods in `SurfStatsApiImpl`**

Add these imports to `surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/SurfStatsApiImpl.kt`:

```kotlin
import dev.slne.surf.stats.api.model.PlayerAdvancements
import dev.slne.surf.stats.core.client.repository.PlayerAdvancementsRepository
import dev.slne.surf.stats.core.client.service.AdvancementSyncStateService
import dev.slne.surf.stats.core.common.packets.SaveAdvancementsRequestPacket
```

Replace the existing `onPlayerQuit` with:

```kotlin
    suspend fun onPlayerQuit(playerUuid: UUID) {
        processPlayerStats(playerUuid)
        processPlayerAdvancements(playerUuid)
        StatisticsManagerService.untrackPlayer(playerUuid)
        AdvancementSyncStateService.forget(playerUuid)
    }
```

Add the new methods after `saveDiffStats`:

```kotlin
    override suspend fun getPlayerAdvancements(playerUuid: UUID): PlayerAdvancements =
        PlayerAdvancementsRepository.loadAdvancements(playerUuid)

    override suspend fun saveAdvancements(
        playerUuid: UUID,
        advancements: PlayerAdvancements,
    ) {
        if (advancements.isEmpty()) {
            return
        }

        statsInstance.rabbitApi.sendRequest(
            SaveAdvancementsRequestPacket(players = listOf(advancements))
        )
    }

    override suspend fun processPlayerAdvancements(playerUuid: UUID) {
        processAllPlayerAdvancements(setOf(playerUuid))
    }

    override suspend fun processAllPlayerAdvancements(uuids: Set<UUID>) {
        if (uuids.isEmpty()) {
            return
        }

        runCatching {
            val snapshots = PlayerAdvancementsRepository.loadAllAdvancements(uuids)
                .filter { it.isNotEmpty() }

            val changed = AdvancementSyncStateService.selectChanged(snapshots)
            if (changed.isEmpty()) {
                return@runCatching
            }

            val response = statsInstance.rabbitApi.sendRequest(
                SaveAdvancementsRequestPacket(players = changed)
            )
            val failed = response.value.toSet()

            if (failed.isNotEmpty()) {
                log.atWarning()
                    .log("Failed to save advancements for ${failed.size}/${changed.size} players")
            }

            AdvancementSyncStateService.markSynced(sent = changed, failed = failed)
        }.onFailure { error ->
            log.atSevere()
                .withCause(error)
                .log("Failed to process advancements for ${uuids.size} players")
        }
    }
```

The empty-snapshot filter is the guard that stops a failed read from deleting stored rows.

- [ ] **Step 6: Verify the build compiles**

Run: `./gradlew :surf-stats-core:surf-stats-core-client:build`
Expected: BUILD SUCCESSFUL, all existing tests still pass.

- [ ] **Step 7: Commit**

```bash
git add surf-stats-api/src/main/kotlin/dev/slne/surf/stats/api/SurfStatsApi.kt \
        surf-stats-core/surf-stats-core-common/src/main/kotlin/dev/slne/surf/stats/core/common/packets/SaveAdvancementsRequestPacket.kt \
        surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/repository/ \
        surf-stats-core/surf-stats-core-client/src/main/kotlin/dev/slne/surf/stats/core/client/SurfStatsApiImpl.kt
git commit -m "✨ feat(api): expose advancement synchronisation"
```

---

### Task 7: Extract the grouped-save helper in the microservice

**Files:**
- Create: `surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/db/GroupedSaver.kt`
- Modify: `surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/db/StatsDatabaseService.kt:270-341`

**Interfaces:**
- Consumes: nothing new.
- Produces: `internal suspend fun <T> saveGrouped(items, operationName, concurrency, playerUuidOf, groupKeyOf, save): Set<UUID>` — used by Task 9.

Pure refactor, no behaviour change. `StatsDatabaseService.saveGroupedBatches` is typed to `PlayerStatsBatch`; the advancement path needs the identical ordering and concurrency semantics for a different type.

- [ ] **Step 1: Create the extracted helper**

```kotlin
package dev.slne.surf.stats.microservice.db

import dev.slne.surf.api.core.util.logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import java.util.*

private val log = logger()

/**
 * Persists [items] while preserving their order within a group.
 *
 * Items sharing a [groupKeyOf] value are processed sequentially so that older
 * values cannot overwrite newer ones. Different groups are processed
 * concurrently up to [concurrency].
 *
 * @param operationName the operation name used in failure log messages
 * @return the UUIDs of players for which at least one item failed to save
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal suspend fun <T> saveGrouped(
    items: List<T>,
    operationName: String,
    concurrency: Int = DEFAULT_CONCURRENCY,
    playerUuidOf: (T) -> UUID,
    groupKeyOf: (T) -> Any,
    save: suspend (T) -> Unit
): Set<UUID> = items
    .groupBy(groupKeyOf)
    .values
    .asFlow()
    .flatMapMerge(concurrency = concurrency) { group ->
        flow {
            for (item in group) {
                val playerUuid = playerUuidOf(item)

                val failed = runCatching { save(item) }
                    .onFailure { exception ->
                        log.atSevere()
                            .withCause(exception)
                            .log("Failed to save $operationName for player $playerUuid")
                    }
                    .isFailure

                if (failed) {
                    emit(playerUuid)
                }
            }
        }
    }
    .toSet()
```

- [ ] **Step 2: Rewrite `saveBatches` and `saveDiffBatches` to use it**

In `StatsDatabaseService.kt`, replace the bodies of `saveBatches` and `saveDiffBatches` (keeping their KDoc) and delete the private `saveGroupedBatches` function entirely:

```kotlin
    suspend fun saveBatches(
        batches: List<PlayerStatsBatch>
    ): Set<UUID> = saveGrouped(
        items = batches,
        operationName = "stats",
        playerUuidOf = { it.playerUuid },
        groupKeyOf = { it.playerUuid to it.stats.serverName },
        save = ::saveBatch
    )

    suspend fun saveDiffBatches(
        batches: List<PlayerStatsBatch>
    ): Set<UUID> = saveGrouped(
        items = batches,
        operationName = "diff stats",
        playerUuidOf = { it.playerUuid },
        groupKeyOf = { it.playerUuid to it.stats.serverName },
        save = ::saveDiffBatch
    )
```

The grouping key `it.playerUuid to it.stats.serverName` is copied verbatim from the deleted function — do not change it.

- [ ] **Step 3: Clean up now-unused imports**

Remove `kotlinx.coroutines.ExperimentalCoroutinesApi` and `kotlinx.coroutines.FlowPreview` from `StatsDatabaseService.kt`. **Keep** `kotlinx.coroutines.flow.*` — `getOptOut` and `getOptOutKeys` still call `.map { }.toList()` / `.toSet()` on the flows returned by Exposed R2DBC queries.

- [ ] **Step 4: Verify the build compiles**

Run: `./gradlew :surf-stats-microservice:build`
Expected: BUILD SUCCESSFUL, no warnings about unused imports.

- [ ] **Step 5: Commit**

```bash
git add surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/db/
git commit -m "♻️ refactor(microservice): extract grouped save helper"
```

---

### Task 8: Microservice tables

**Files:**
- Create: `surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/db/tables/AdvancementsTable.kt`
- Create: `surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/db/tables/PlayerAdvancementsTable.kt`
- Create: `surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/db/tables/PlayerAdvancementCriteriaTable.kt`

**Interfaces:**
- Consumes: existing `ServersTable`.
- Produces: `AdvancementsTable.name`; `PlayerAdvancementsTable.{playerUuid, advancementName, serverName, done, completedAt, criteriaDone, updatedAt}`; `PlayerAdvancementCriteriaTable.{playerUuid, advancementName, criterionName, serverName, awardedAt}`. Used by Task 9.

Table definitions only — the operator creates the actual tables from the DDL in the spec. There is no schema-creation code anywhere in this repository; do not add any.

- [ ] **Step 1: Create `AdvancementsTable`**

```kotlin
package dev.slne.surf.stats.microservice.db.tables

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table

object AdvancementsTable : Table("advancements") {
    val name = varchar("name", 255).transform({ key(it) }, { it.asString() })

    override val primaryKey = PrimaryKey(name)
}
```

- [ ] **Step 2: Create `PlayerAdvancementsTable`**

```kotlin
package dev.slne.surf.stats.microservice.db.tables

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.timestamp

object PlayerAdvancementsTable : Table("player_advancements") {
    val playerUuid = nativeUuid("player_uuid")
    val advancementName = varchar("advancement_name", 255)
        .transform({ key(it) }, { it.asString() })
        .references(AdvancementsTable.name)
    val serverName = varchar("server_name", 128).references(ServersTable.name)
    val done = bool("done").default(false)

    /** Latest criterion timestamp once the advancement is complete, else `null`. */
    val completedAt = timestamp("completed_at").nullable()

    /** Number of awarded criteria. The total is not available from the source file. */
    val criteriaDone = integer("criteria_done").default(0)

    /**
     * When this player's snapshot last changed. Snapshot-scoped, not row-scoped:
     * the whole snapshot is rewritten as a unit, so every row of a player shares
     * this value. The per-advancement timestamp is [completedAt].
     */
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(playerUuid, advancementName, serverName)

    init {
        index("idx_player_advancements_player_done", false, playerUuid, done)
        index(
            "idx_player_advancements_advancement_done",
            false,
            advancementName,
            done,
            completedAt
        )
    }
}
```

- [ ] **Step 3: Create `PlayerAdvancementCriteriaTable`**

```kotlin
package dev.slne.surf.stats.microservice.db.tables

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.timestamp

object PlayerAdvancementCriteriaTable : Table("player_advancement_criteria") {
    val playerUuid = nativeUuid("player_uuid")
    val advancementName = varchar("advancement_name", 255)
        .transform({ key(it) }, { it.asString() })
        .references(AdvancementsTable.name)

    /**
     * Criterion names are free-form strings, so this stays a plain varchar —
     * `Key.key()` throws on names datapacks are allowed to use.
     */
    val criterionName = varchar("criterion_name", 128)
    val serverName = varchar("server_name", 128).references(ServersTable.name)

    /** `null` when the timestamp in the source file could not be parsed. */
    val awardedAt = timestamp("awarded_at").nullable()

    override val primaryKey = PrimaryKey(playerUuid, advancementName, criterionName, serverName)
}
```

- [ ] **Step 4: Verify the build compiles**

Run: `./gradlew :surf-stats-microservice:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/db/tables/
git commit -m "✨ feat(microservice): add advancement tables"
```

---

### Task 9: Microservice persistence and packet handler

**Files:**
- Create: `surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/db/AdvancementsDatabaseService.kt`
- Create: `surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/handler/AdvancementsPacketHandler.kt`
- Modify: `surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/StatsMicroservice.kt:19-24`

**Interfaces:**
- Consumes: `saveGrouped` (Task 7), the three tables (Task 8), `SaveAdvancementsRequestPacket` (Task 6), existing `StatsUuidResponsePacket`.
- Produces: `AdvancementsDatabaseService.saveSnapshots(snapshots: List<PlayerAdvancements>): Set<UUID>`.

No unit tests: this repository has no R2DBC test harness, and none is introduced here. Verification is compilation plus the manual database check in Task 11.

- [ ] **Step 1: Create the database service**

```kotlin
package dev.slne.surf.stats.microservice.db

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.*
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.stats.api.model.PlayerAdvancements
import dev.slne.surf.stats.microservice.db.tables.AdvancementsTable
import dev.slne.surf.stats.microservice.db.tables.PlayerAdvancementCriteriaTable
import dev.slne.surf.stats.microservice.db.tables.PlayerAdvancementsTable
import java.util.*

/**
 * Persists player advancement snapshots.
 *
 * A snapshot is always complete, so it is written by replacement: everything
 * stored for `(player, server)` is deleted and re-inserted inside one
 * transaction. Advancements can disappear — `/advancement revoke`, a removed
 * datapack, a reset world — and an upsert-only strategy would leave those rows
 * behind forever. Since no history is kept, replacement is both simpler than a
 * `NOT IN` diff and portable across dialects.
 */
object AdvancementsDatabaseService {

    /**
     * Saves advancement snapshots.
     *
     * Snapshots for the same player and server are saved sequentially; unrelated
     * groups may be processed concurrently.
     *
     * @return the UUIDs of players whose snapshot failed to save
     */
    suspend fun saveSnapshots(snapshots: List<PlayerAdvancements>): Set<UUID> = saveGrouped(
        items = snapshots,
        operationName = "advancements",
        playerUuidOf = { it.playerUuid },
        groupKeyOf = { it.playerUuid to it.serverName },
        save = ::saveSnapshot
    )

    private suspend fun saveSnapshot(snapshot: PlayerAdvancements) {
        // An empty snapshot means the file could not be read. Replacing with
        // nothing would delete the player's stored advancements.
        if (snapshot.isEmpty()) {
            return
        }

        suspendTransaction {
            AdvancementsTable.batchInsert(
                data = snapshot.map { it.advancement }.toSet(),
                ignore = true,
                shouldReturnGeneratedValues = false,
            ) { advancement ->
                this[AdvancementsTable.name] = advancement
            }

            PlayerAdvancementCriteriaTable.deleteWhere {
                (PlayerAdvancementCriteriaTable.playerUuid eq snapshot.playerUuid) and
                        (PlayerAdvancementCriteriaTable.serverName eq snapshot.serverName)
            }

            PlayerAdvancementsTable.deleteWhere {
                (PlayerAdvancementsTable.playerUuid eq snapshot.playerUuid) and
                        (PlayerAdvancementsTable.serverName eq snapshot.serverName)
            }

            PlayerAdvancementsTable.batchInsert(
                data = snapshot.advancements,
                shouldReturnGeneratedValues = false,
            ) { entry ->
                this[PlayerAdvancementsTable.playerUuid] = snapshot.playerUuid
                this[PlayerAdvancementsTable.advancementName] = entry.advancement
                this[PlayerAdvancementsTable.serverName] = snapshot.serverName
                this[PlayerAdvancementsTable.done] = entry.done
                this[PlayerAdvancementsTable.completedAt] = entry.completedAt
                this[PlayerAdvancementsTable.criteriaDone] = entry.criteriaDone
            }

            val criteriaRows = snapshot.advancements.flatMap { entry ->
                entry.criteria.map { criterion -> entry.advancement to criterion }
            }

            if (criteriaRows.isNotEmpty()) {
                PlayerAdvancementCriteriaTable.batchInsert(
                    data = criteriaRows,
                    shouldReturnGeneratedValues = false,
                ) { (advancement, criterion) ->
                    this[PlayerAdvancementCriteriaTable.playerUuid] = snapshot.playerUuid
                    this[PlayerAdvancementCriteriaTable.advancementName] = advancement
                    this[PlayerAdvancementCriteriaTable.criterionName] = criterion.name
                    this[PlayerAdvancementCriteriaTable.serverName] = snapshot.serverName
                    this[PlayerAdvancementCriteriaTable.awardedAt] = criterion.awardedAt
                }
            }
        }
    }
}
```

The `advancements` dimension rows are inserted first because both detail tables have a foreign key onto them.

- [ ] **Step 2: Create the packet handler**

```kotlin
package dev.slne.surf.stats.microservice.handler

import dev.slne.surf.rabbitmq.api.handler.RabbitHandler
import dev.slne.surf.stats.core.common.packets.SaveAdvancementsRequestPacket
import dev.slne.surf.stats.core.common.packets.StatsUuidResponsePacket
import dev.slne.surf.stats.microservice.db.AdvancementsDatabaseService
import kotlinx.coroutines.launch

object AdvancementsPacketHandler {
    @RabbitHandler
    fun handleSaveAdvancementsRequest(request: SaveAdvancementsRequestPacket) = request.launch {
        val failed = AdvancementsDatabaseService.saveSnapshots(request.players).toList()

        request.respond(StatsUuidResponsePacket(failed))
    }
}
```

- [ ] **Step 3: Register the handler**

In `StatsMicroservice.kt`, add the import and one registration line:

```kotlin
import dev.slne.surf.stats.microservice.handler.AdvancementsPacketHandler
```

```kotlin
    override suspend fun onBootstrap(args: List<String>) {
        rabbitApi.registerRequestHandler(StatsPacketHandler)
        rabbitApi.registerRequestHandler(ServerPacketHandler)
        rabbitApi.registerRequestHandler(OptOutPacketHandler)
        rabbitApi.registerRequestHandler(AdvancementsPacketHandler)
        rabbitApi.freezeAndConnect()
    }
```

- [ ] **Step 4: Verify the build compiles**

Run: `./gradlew :surf-stats-microservice:build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add surf-stats-microservice/src/main/kotlin/dev/slne/surf/stats/microservice/
git commit -m "✨ feat(microservice): persist advancement snapshots"
```

---

### Task 10: Paper plugin trigger points

**Files:**
- Create: `surf-stats-paper/src/main/kotlin/dev/slne/surf/stats/paper/listener/WorldSaveListener.kt`
- Modify: `surf-stats-paper/src/main/kotlin/dev/slne/surf/stats/paper/SurfStatsPlugin.kt`

**Interfaces:**
- Consumes: `AdvancementsFileService.initialize` (Task 4), `SurfStatsApi.processAllPlayerAdvancements` (Task 6), existing `StatisticsManagerService.trackedPlayers`.
- Produces: `SurfStatsPlugin.saveTrackedPlayerAdvancements()` — called by `WorldSaveListener` and `onDisableAsync`. Also replaces the private `flushAllPlayerStats` / `flushPlayerStats` pair with `flushAll(playerUuids, accessor)` / `flushViaReflection(player, accessor)`.

Quit is already covered: `PlayerStatsListener.onPlayerQuit` calls `surfStatsApiImpl.onPlayerQuit`, which Task 6 extended. That file needs no change.

- [ ] **Step 1: Initialize the file service**

In `SurfStatsPlugin.initializeServices()`, after the existing `StatsFileService.initialize(statsDirectory)` call:

```kotlin
        // stats/ and advancements/ are siblings inside the world directory; the
        // NMS bridge exposes only the stats path.
        val advancementsDirectory = statsDirectory.resolveSibling("advancements")
        log.atInfo().log("Advancements directory: $advancementsDirectory")

        AdvancementsFileService.initialize(advancementsDirectory)
```

Add the import:

```kotlin
import dev.slne.surf.stats.core.client.json.AdvancementsFileService
```

- [ ] **Step 2: Generalise the reflection flush helper**

The advancement flush differs from the existing stats flush by one method name, so
generalise rather than copy. Replace the existing `flushAllPlayerStats` and
`flushPlayerStats` in `SurfStatsPlugin` with:

```kotlin
    private fun flushAll(playerUuids: Set<UUID>, accessor: String) {
        playerUuids.forEach { uuid ->
            server.getPlayer(uuid)?.let { flushViaReflection(it, accessor) }
        }
    }

    /**
     * Forces Minecraft to write one of the player's data files to disk.
     *
     * Uses reflection to call CraftPlayer -> ServerPlayer -> [accessor]() -> save(),
     * which under Paper's Mojang mappings reaches `ServerStatsCounter.save()` for
     * [STATS_ACCESSOR] and `PlayerAdvancements.save()` for [ADVANCEMENTS_ACCESSOR].
     */
    private fun flushViaReflection(player: Player, accessor: String) {
        try {
            val handle = player.javaClass.getMethod("getHandle").invoke(player)
            val target = handle.javaClass.getMethod(accessor).invoke(handle)
            target.javaClass.getMethod("save").invoke(target)
        } catch (e: Exception) {
            log.atWarning().withCause(e).log("Failed to flush $accessor for ${player.name}")
        }
    }
```

Update the existing call site inside `saveTrackedPlayerStats`:

```kotlin
            flushAll(trackedPlayers, STATS_ACCESSOR)
```

Add both accessor names to the existing `companion object`:

```kotlin
    companion object {
        private val SAVE_INTERVAL = 5.minutes

        private const val STATS_ACCESSOR = "getStats"
        private const val ADVANCEMENTS_ACCESSOR = "getAdvancements"
    }
```

- [ ] **Step 2b: Add the advancement sync entry point**

Add to `SurfStatsPlugin`, next to the existing `saveTrackedPlayerStats`:

```kotlin
    /**
     * Flushes and ships advancements for every tracked player.
     *
     * Called from [WorldSaveListener] and on shutdown. Players whose snapshot is
     * unchanged are filtered out inside the API.
     */
    suspend fun saveTrackedPlayerAdvancements() {
        val trackedPlayers = StatisticsManagerService.trackedPlayers

        if (trackedPlayers.isEmpty()) {
            return
        }

        log.atInfo().log("Saving advancements for ${trackedPlayers.size} players")

        flushAll(trackedPlayers, ADVANCEMENTS_ACCESSOR)
        SurfStatsApi.processAllPlayerAdvancements(trackedPlayers)
    }
```

- [ ] **Step 3: Create the debounced world-save listener**

```kotlin
package dev.slne.surf.stats.paper.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.stats.paper.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldSaveEvent
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds

/**
 * Syncs advancements whenever the server saves.
 *
 * Minecraft writes player advancement files as part of its save cycle, so this
 * is the point at which the files on disk are fresh.
 *
 * [WorldSaveEvent] fires once per world, so a server with three worlds fires
 * three times per autosave. Events arriving within [DEBOUNCE] of the last
 * handled one are ignored.
 */
object WorldSaveListener : Listener {
    private val DEBOUNCE = 30.seconds.inWholeMilliseconds

    private val lastRun = AtomicLong(0)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldSave(event: WorldSaveEvent) {
        val now = System.currentTimeMillis()
        val previous = lastRun.get()

        if (now - previous < DEBOUNCE) {
            return
        }

        if (!lastRun.compareAndSet(previous, now)) {
            return
        }

        plugin.launch {
            plugin.saveTrackedPlayerAdvancements()
        }
    }
}
```

- [ ] **Step 4: Register the listener**

In `SurfStatsPlugin.registerListeners()`:

```kotlin
    private fun registerListeners() {
        PlayerStatsListener.register()
        WorldSaveListener.register()
    }
```

Add the import:

```kotlin
import dev.slne.surf.stats.paper.listener.WorldSaveListener
```

- [ ] **Step 5: Sync on shutdown**

In `SurfStatsPlugin.onDisableAsync()`, after the existing `saveTrackedPlayerStats()` call and before `statsInstance.onDisable()`:

```kotlin
        saveTrackedPlayerAdvancements()
```

- [ ] **Step 6: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL across all modules, all tests pass.

- [ ] **Step 7: Commit**

```bash
git add surf-stats-paper/src/main/kotlin/dev/slne/surf/stats/paper/
git commit -m "✨ feat(paper): sync advancements on quit, world save and shutdown"
```

---

### Task 11: Documentation and manual verification

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: everything above.
- Produces: nothing.

- [ ] **Step 1: Update the "How It Works" section**

Replace the paragraph beginning "The Paper plugin reads Minecraft's native `<world>/stats/<uuid>.json` files" and the list that follows with:

```markdown
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
```

- [ ] **Step 2: Update the database schema table**

Append these rows to the table in the *Database Schema* section:

```markdown
| `advancements` | Distinct advancement identifiers (e.g. `minecraft:story/root`) |
| `player_advancements` | Current advancement state per `(player, advancement, server)` — `done`, `completed_at`, `criteria_done` — replaced as a whole snapshot |
| `player_advancement_criteria` | Awarded criteria per advancement with `awarded_at` |
```

- [ ] **Step 3: Document the API additions**

Append to the *Interface* method table in the *API Usage* section:

```markdown
| `getPlayerAdvancements` | Load the player's current advancement snapshot from disk (recipes excluded) |
| `saveAdvancements` | Replace everything stored for `(player, server)` with the given snapshot — must be complete; empty snapshots are ignored |
| `processPlayerAdvancements` | Load one player's advancements and send them if they changed |
| `processAllPlayerAdvancements` | Same, batched for many players |
```

And add the four signatures to the `interface SurfStatsApi` code block in that section:

```kotlin
    suspend fun getPlayerAdvancements(playerUuid: UUID): PlayerAdvancements
    suspend fun saveAdvancements(playerUuid: UUID, advancements: PlayerAdvancements)
    suspend fun processPlayerAdvancements(playerUuid: UUID)
    suspend fun processAllPlayerAdvancements(uuids: Set<UUID>)
```

- [ ] **Step 4: Run the full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Manual verification against a running stack**

The database layer has no automated coverage, so verify it by hand:

1. Apply the DDL from `docs/superpowers/specs/2026-08-09-advancement-sync-design.md` to the Postgres database.
2. Start the microservice and a Paper server with the plugin.
3. Join, earn an advancement (`/advancement grant <player> only minecraft:story/mine_diamond`), and quit.
4. Check the rows:
   ```sql
   SELECT advancement_name, done, completed_at, criteria_done
   FROM player_advancements
   WHERE player_uuid = '<uuid>';

   SELECT advancement_name, criterion_name, awarded_at
   FROM player_advancement_criteria
   WHERE player_uuid = '<uuid>';
   ```
   Expect no `recipes/…` rows, `done = true` and a non-null `completed_at` for the granted advancement.
5. Rejoin, run `/advancement revoke <player> only minecraft:story/mine_diamond`, quit, and confirm the row is **gone** rather than stale.
6. Rejoin and quit without earning anything; confirm the plugin log shows no advancement send (the hash guard suppressed it).

- [ ] **Step 6: Commit**

```bash
git add README.md
git commit -m "📝 docs: document advancement synchronisation"
```

---

## Verification Checklist

- [ ] `./gradlew build` passes
- [ ] All new unit tests pass (36 tests across 5 test classes)
- [ ] No `recipes/…` rows in `player_advancements`
- [ ] A revoked advancement disappears from the database
- [ ] An unchanged snapshot produces no send
- [ ] A deleted or corrupted advancement file leaves stored rows untouched
