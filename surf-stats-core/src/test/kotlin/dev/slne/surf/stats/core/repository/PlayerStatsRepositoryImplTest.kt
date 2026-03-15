package dev.slne.surf.stats.core.repository

import dev.slne.surf.stats.core.service.StatsFileService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class PlayerStatsRepositoryImplTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var statsDir: Path

    private val simpleStatsJson =
        """{"stats": {"minecraft:mined": {"minecraft:stone": 100}}, "DataVersion": 3953}"""

    @BeforeEach
    fun setup() = runTest {
        statsDir = tempDir.resolve("stats")
        Files.createDirectories(statsDir)
        StatsFileService.initialize(statsDir)
    }

    @Test
    fun `should load stats for existing player`() = runTest {
        val uuid = UUID.randomUUID()
        Files.writeString(statsDir.resolve("$uuid.json"), simpleStatsJson)

        val result = PlayerStatsRepository.loadStats(uuid, "TestPlayer")

        assertNotNull(result)
        assertEquals(uuid, result?.uuid)
        assertEquals("TestPlayer", result?.name)
        assertEquals(3953, result?.dataVersion)
        assertEquals(1, result?.stats?.size)
    }

    @Test
    fun `should return null for non-existent player`() = runTest {
        val uuid = UUID.randomUUID()

        val result = PlayerStatsRepository.loadStats(uuid)

        assertNull(result)
    }

    @Test
    fun `should load all stats for multiple players with names`() = runTest {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val uuid3 = UUID.randomUUID()

        Files.writeString(statsDir.resolve("$uuid1.json"), simpleStatsJson)
        Files.writeString(statsDir.resolve("$uuid2.json"), simpleStatsJson)
        // uuid3 intentionally has no file

        val players = mapOf(
            uuid1 to "Player1",
            uuid2 to "Player2",
            uuid3 to "Player3"
        )
        val result = PlayerStatsRepository.loadAllStats(players)

        assertEquals(2, result.size)
        assertTrue(result.any { it.uuid == uuid1 })
        assertTrue(result.any { it.uuid == uuid2 })
        assertFalse(result.any { it.uuid == uuid3 })
    }

    @Test
    fun `should check if stats exist`() = runTest {
        val existingUuid = UUID.randomUUID()
        val nonExistingUuid = UUID.randomUUID()

        Files.writeString(statsDir.resolve("$existingUuid.json"), simpleStatsJson)

        assertTrue(PlayerStatsRepository.statsExist(existingUuid))
        assertFalse(PlayerStatsRepository.statsExist(nonExistingUuid))
    }

    @Test
    fun `should preserve stat entries structure`() = runTest {
        val uuid = UUID.randomUUID()
        Files.writeString(
            statsDir.resolve("$uuid.json"), """
            {
                "stats": {
                    "minecraft:mined": {"minecraft:stone": 100, "minecraft:dirt": 50},
                    "minecraft:killed": {"minecraft:zombie": 25}
                },
                "DataVersion": 3953
            }
        """.trimIndent()
        )

        val result = PlayerStatsRepository.loadStats(uuid, "TestPlayer")

        assertNotNull(result)
        assertEquals(3, result?.stats?.size)
        assertEquals(2, result?.categories()?.size)
        assertEquals(100L, result?.getStat("minecraft:mined", "minecraft:stone"))
        assertEquals(25L, result?.getStat("minecraft:killed", "minecraft:zombie"))
    }
}
