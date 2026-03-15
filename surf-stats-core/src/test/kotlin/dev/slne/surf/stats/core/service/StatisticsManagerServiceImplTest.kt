package dev.slne.surf.stats.core.service

import dev.slne.surf.stats.api.service.StatisticsManagerService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class StatisticsManagerServiceImplTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var statsDir: Path
    private val service: StatisticsManagerService = StatisticsManagerServiceImpl()

    private val uuid: UUID = UUID.randomUUID()
    private val playerName = "TestPlayer"

    @BeforeEach
    fun setup() = runTest {
        statsDir = tempDir.resolve("stats")
        Files.createDirectories(statsDir)
        StatsFileService.initialize(statsDir)
    }

    private fun writeStats(uuid: UUID, stats: Map<String, Map<String, Long>>, dataVersion: Int = 3953) {
        val statsJson = stats.entries.joinToString(",") { (category, entries) ->
            val entriesJson = entries.entries.joinToString(",") { (key, value) -> "\"$key\":$value" }
            "\"$category\":{$entriesJson}"
        }
        Files.writeString(statsDir.resolve("$uuid.json"), """{"stats":{$statsJson},"DataVersion":$dataVersion}""")
    }

    @Nested
    inner class TrackPlayer {
        @Test
        fun `should load snapshot into memory`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))

            service.trackPlayer(uuid, playerName)

            assertTrue(service.isTracking(uuid))
            val snapshot = service.snapshotMap[uuid]
            assertNotNull(snapshot)
            assertEquals(100L, snapshot?.getStat("minecraft:mined", "minecraft:stone"))
        }

        @Test
        fun `should track player with empty stats when file does not exist`() = runTest {
            service.trackPlayer(uuid, playerName)

            assertTrue(service.isTracking(uuid))
            val snapshot = service.snapshotMap[uuid]
            assertNotNull(snapshot)
            assertTrue(snapshot!!.stats.isEmpty())
        }
    }

    @Nested
    inner class ComputeDiffs {
        @Test
        fun `should return diffs when stats increased`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)

            // Simulate stats increasing
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 150L)))
            val diffs = service.computeDiffs(uuid, playerName)

            assertEquals(1, diffs.size)
            assertEquals("minecraft:mined", diffs[0].category)
            assertEquals("minecraft:stone", diffs[0].key)
            assertEquals(50L, diffs[0].value)
        }

        @Test
        fun `should return empty list when stats unchanged`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)

            // Stats stay the same
            val diffs = service.computeDiffs(uuid, playerName)

            assertTrue(diffs.isEmpty())
        }

        @Test
        fun `should ignore negative diffs`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)

            // Stats decrease (e.g. world reset)
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 50L)))
            val diffs = service.computeDiffs(uuid, playerName)

            assertTrue(diffs.isEmpty())
        }

        @Test
        fun `should handle new stat keys appearing`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)

            // New stat key added
            writeStats(uuid, mapOf(
                "minecraft:mined" to mapOf("minecraft:stone" to 100L, "minecraft:dirt" to 30L)
            ))
            val diffs = service.computeDiffs(uuid, playerName)

            assertEquals(1, diffs.size)
            assertEquals("minecraft:dirt", diffs[0].key)
            assertEquals(30L, diffs[0].value)
        }

        @Test
        fun `should handle new category appearing`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)

            writeStats(uuid, mapOf(
                "minecraft:mined" to mapOf("minecraft:stone" to 100L),
                "minecraft:killed" to mapOf("minecraft:zombie" to 5L)
            ))
            val diffs = service.computeDiffs(uuid, playerName)

            assertEquals(1, diffs.size)
            assertEquals("minecraft:killed", diffs[0].category)
            assertEquals("minecraft:zombie", diffs[0].key)
            assertEquals(5L, diffs[0].value)
        }

        @Test
        fun `should compute diffs for multiple stats at once`() = runTest {
            writeStats(uuid, mapOf(
                "minecraft:mined" to mapOf("minecraft:stone" to 100L, "minecraft:dirt" to 50L),
                "minecraft:killed" to mapOf("minecraft:zombie" to 10L)
            ))
            service.trackPlayer(uuid, playerName)

            writeStats(uuid, mapOf(
                "minecraft:mined" to mapOf("minecraft:stone" to 200L, "minecraft:dirt" to 50L),
                "minecraft:killed" to mapOf("minecraft:zombie" to 15L)
            ))
            val diffs = service.computeDiffs(uuid, playerName)

            assertEquals(2, diffs.size)
            val stone = diffs.find { it.key == "minecraft:stone" }
            val zombie = diffs.find { it.key == "minecraft:zombie" }
            assertEquals(100L, stone?.value)
            assertEquals(5L, zombie?.value)
        }

        @Test
        fun `should return empty list when file does not exist`() = runTest {
            service.trackPlayer(uuid, playerName)

            // File doesn't exist — nothing to diff against
            val diffs = service.computeDiffs(uuid, playerName)

            assertTrue(diffs.isEmpty())
        }

        @Test
        fun `should not update snapshot`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)

            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 150L)))
            service.computeDiffs(uuid, playerName)

            // Snapshot should still be the old value since computeDiffs doesn't update it
            val snapshot = service.snapshotMap[uuid]
            assertEquals(100L, snapshot?.getStat("minecraft:mined", "minecraft:stone"))

            // Computing again should return the same diff
            val diffs2 = service.computeDiffs(uuid, playerName)
            assertEquals(1, diffs2.size)
            assertEquals(50L, diffs2[0].value)
        }

        @Test
        fun `should treat untracked player as empty snapshot`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))

            // Not tracked — no snapshot exists
            val diffs = service.computeDiffs(uuid, playerName)

            assertEquals(1, diffs.size)
            assertEquals(100L, diffs[0].value)
        }
    }

    @Nested
    inner class UpdateSnapshot {
        @Test
        fun `should update snapshot to current file values`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)

            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 150L)))
            service.updateSnapshot(uuid, playerName)

            val snapshot = service.snapshotMap[uuid]
            assertEquals(150L, snapshot?.getStat("minecraft:mined", "minecraft:stone"))
        }

        @Test
        fun `should reset diffs after update`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)

            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 150L)))
            val diffs1 = service.computeDiffs(uuid, playerName)
            assertEquals(50L, diffs1[0].value)

            service.updateSnapshot(uuid, playerName)

            // After updating, diff should be zero
            val diffs2 = service.computeDiffs(uuid, playerName)
            assertTrue(diffs2.isEmpty())
        }

        @Test
        fun `should allow accumulating new diffs after update`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)

            // First cycle: +50
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 150L)))
            val diffs1 = service.computeDiffs(uuid, playerName)
            assertEquals(50L, diffs1[0].value)
            service.updateSnapshot(uuid, playerName)

            // Second cycle: +30
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 180L)))
            val diffs2 = service.computeDiffs(uuid, playerName)
            assertEquals(1, diffs2.size)
            assertEquals(30L, diffs2[0].value)
        }
    }

    @Nested
    inner class UntrackPlayer {
        @Test
        fun `should remove player from tracking`() = runTest {
            writeStats(uuid, mapOf("minecraft:mined" to mapOf("minecraft:stone" to 100L)))
            service.trackPlayer(uuid, playerName)
            assertTrue(service.isTracking(uuid))

            service.untrackPlayer(uuid)

            assertFalse(service.isTracking(uuid))
            assertNull(service.snapshotMap[uuid])
        }

        @Test
        fun `should be safe to untrack non-tracked player`() {
            assertDoesNotThrow { service.untrackPlayer(UUID.randomUUID()) }
        }
    }
}
