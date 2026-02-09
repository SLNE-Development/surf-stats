package dev.slne.surf.stats.core.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class StatsFileServiceImplTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var statsDir: Path
    private lateinit var service: StatsFileServiceImpl

    @BeforeEach
    fun setup() = runTest {
        statsDir = tempDir.resolve("stats")
        Files.createDirectories(statsDir)
        service = StatsFileServiceImpl()
        service.initialize(statsDir)
    }

    @AfterEach
    fun cleanup() {
        // Cleanup is handled by @TempDir
    }

    @Test
    fun `should initialize with existing stats directory`() = runTest {
        assertTrue(Files.exists(statsDir))
        assertTrue(Files.isDirectory(statsDir))
    }

    @Test
    fun `should return correct stats file path`() {
        val uuid = UUID.randomUUID()
        val path = service.getStatsFilePath(uuid)

        assertEquals(statsDir.resolve("$uuid.json"), path)
    }

    @Test
    fun `should load valid stats file with player name`() = runTest {
        val uuid = UUID.randomUUID()
        val playerName = "TestPlayer"
        val statsContent = """
            {
                "stats": {
                    "minecraft:mined": {
                        "minecraft:stone": 500
                    },
                    "minecraft:custom": {
                        "minecraft:play_time": 72000
                    }
                },
                "DataVersion": 3953
            }
        """.trimIndent()

        Files.writeString(statsDir.resolve("$uuid.json"), statsContent)

        val result = service.loadStatistics(uuid, playerName)

        assertTrue(result.isSuccess)
        val stats = result.getOrNull()
        assertNotNull(stats)
        assertEquals(uuid, stats?.uuid)
        assertEquals(playerName, stats?.name)
        assertEquals(3953, stats?.dataVersion)
        assertEquals(2, stats?.stats?.size)

        // Verify stat entries
        val stoneEntry = stats?.stats?.find { it.category == "minecraft:mined" && it.key == "minecraft:stone" }
        assertNotNull(stoneEntry)
        assertEquals(500L, stoneEntry?.value)

        val playTimeEntry = stats?.stats?.find { it.category == "minecraft:custom" && it.key == "minecraft:play_time" }
        assertNotNull(playTimeEntry)
        assertEquals(72000L, playTimeEntry?.value)
    }

    @Test
    fun `should return failure for non-existent file`() = runTest {
        val uuid = UUID.randomUUID()

        val result = service.loadStatistics(uuid)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun `should return failure for invalid JSON`() = runTest {
        val uuid = UUID.randomUUID()
        Files.writeString(statsDir.resolve("$uuid.json"), "not valid json")

        val result = service.loadStatistics(uuid)

        assertTrue(result.isFailure)
    }

    @Test
    fun `should load multiple stats files with names`() = runTest {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val uuid3 = UUID.randomUUID() // This one won't have a file

        val statsContent = """{"stats": {"minecraft:mined": {"minecraft:stone": 100}}, "DataVersion": 3953}"""

        Files.writeString(statsDir.resolve("$uuid1.json"), statsContent)
        Files.writeString(statsDir.resolve("$uuid2.json"), statsContent)

        val players = mapOf(
            uuid1 to "Player1",
            uuid2 to "Player2",
            uuid3 to "Player3"
        )
        val results = service.loadAllStatistics(players)

        assertEquals(3, results.size)
        assertTrue(results[uuid1]?.isSuccess == true)
        assertTrue(results[uuid2]?.isSuccess == true)
        assertTrue(results[uuid3]?.isFailure == true)

        // Verify player names are preserved
        assertEquals("Player1", results[uuid1]?.getOrNull()?.name)
        assertEquals("Player2", results[uuid2]?.getOrNull()?.name)
    }

    @Test
    fun `should handle empty stats file`() = runTest {
        val uuid = UUID.randomUUID()
        Files.writeString(statsDir.resolve("$uuid.json"), """{"stats": {}, "DataVersion": 3953}""")

        val result = service.loadStatistics(uuid)

        assertTrue(result.isSuccess)
        val stats = result.getOrNull()
        assertNotNull(stats)
        assertTrue(stats?.stats?.isEmpty() == true)
        assertEquals(3953, stats?.dataVersion)
    }

    @Test
    fun `should extract categories from loaded stats`() = runTest {
        val uuid = UUID.randomUUID()
        val statsContent = """
            {
                "stats": {
                    "minecraft:mined": {"minecraft:stone": 100},
                    "minecraft:killed": {"minecraft:zombie": 25},
                    "minecraft:custom": {"minecraft:play_time": 72000}
                },
                "DataVersion": 3953
            }
        """.trimIndent()

        Files.writeString(statsDir.resolve("$uuid.json"), statsContent)

        val result = service.loadStatistics(uuid, "TestPlayer")
        val stats = result.getOrNull()!!

        val categories = stats.categories()
        assertEquals(3, categories.size)
        assertTrue(categories.contains("minecraft:mined"))
        assertTrue(categories.contains("minecraft:killed"))
        assertTrue(categories.contains("minecraft:custom"))
    }
}
