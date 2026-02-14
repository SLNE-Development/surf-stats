package dev.slne.surf.stats.core.repository

import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.model.StatEntry
import dev.slne.surf.stats.api.service.StatsFileService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.UUID

class PlayerStatsRepositoryImplTest {

    private lateinit var repository: PlayerStatsRepositoryImpl
    private lateinit var mockFileService: MockStatsFileService

    @BeforeEach
    fun setup() {
        mockFileService = MockStatsFileService()
        repository = PlayerStatsRepositoryImpl(mockFileService)
    }

    @Test
    fun `should load stats for existing player`() = runTest {
        val uuid = UUID.randomUUID()
        val expectedStats = PlayerStats(
            uuid = uuid,
            name = "TestPlayer",
            dataVersion = 3953,
            stats = listOf(
                StatEntry("minecraft:mined", "minecraft:stone", 100L)
            )
        )
        mockFileService.addStats(uuid, expectedStats)

        val result = repository.loadStats(uuid, "TestPlayer")

        assertNotNull(result)
        assertEquals(uuid, result?.uuid)
        assertEquals("TestPlayer", result?.name)
        assertEquals(3953, result?.dataVersion)
        assertEquals(1, result?.stats?.size)
    }

    @Test
    fun `should return null for non-existent player`() = runTest {
        val uuid = UUID.randomUUID()

        val result = repository.loadStats(uuid)

        assertNull(result)
    }

    @Test
    fun `should load all stats for multiple players with names`() = runTest {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val uuid3 = UUID.randomUUID()

        mockFileService.addStats(uuid1, PlayerStats(uuid1, "Player1", 3953, emptyList()))
        mockFileService.addStats(uuid2, PlayerStats(uuid2, "Player2", 3953, emptyList()))
        // uuid3 intentionally not added

        val players = mapOf(
            uuid1 to "Player1",
            uuid2 to "Player2",
            uuid3 to "Player3"
        )
        val result = repository.loadAllStats(players)

        assertEquals(2, result.size)
        assertTrue(result.any { it.uuid == uuid1 })
        assertTrue(result.any { it.uuid == uuid2 })
        assertFalse(result.any { it.uuid == uuid3 })
    }

    @Test
    fun `should check if stats exist`() = runTest {
        val existingUuid = UUID.randomUUID()
        val nonExistingUuid = UUID.randomUUID()

        mockFileService.addStats(existingUuid, PlayerStats(existingUuid, "Player", 3953, emptyList()))

        assertTrue(repository.statsExist(existingUuid))
        assertFalse(repository.statsExist(nonExistingUuid))
    }

    @Test
    fun `should preserve stat entries structure`() = runTest {
        val uuid = UUID.randomUUID()
        val entries = listOf(
            StatEntry("minecraft:mined", "minecraft:stone", 100L),
            StatEntry("minecraft:mined", "minecraft:dirt", 50L),
            StatEntry("minecraft:killed", "minecraft:zombie", 25L)
        )
        val expectedStats = PlayerStats(uuid, "TestPlayer", 3953, entries)
        mockFileService.addStats(uuid, expectedStats)

        val result = repository.loadStats(uuid, "TestPlayer")

        assertNotNull(result)
        assertEquals(3, result?.stats?.size)
        assertEquals(2, result?.categories()?.size)

        // Verify specific stat lookup
        assertEquals(100L, result?.getStat("minecraft:mined", "minecraft:stone"))
        assertEquals(25L, result?.getStat("minecraft:killed", "minecraft:zombie"))
    }
}

/**
 * Mock implementation of StatsFileService for testing.
 */
private class MockStatsFileService : StatsFileService {

    private val statsMap = mutableMapOf<UUID, PlayerStats>()

    fun addStats(uuid: UUID, stats: PlayerStats) {
        statsMap[uuid] = stats
    }

    override suspend fun initialize(statsDirectory: Path) {
        // No-op for mock
    }

    override suspend fun loadStatistics(playerUuid: UUID): Result<PlayerStats> {
        return statsMap[playerUuid]?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Stats not found for $playerUuid"))
    }

    override suspend fun loadStatistics(playerUuid: UUID, playerName: String): Result<PlayerStats> {
        return statsMap[playerUuid]?.let {
            Result.success(it.copy(name = playerName))
        } ?: Result.failure(NoSuchElementException("Stats not found for $playerUuid"))
    }

    override suspend fun loadAllStatistics(playerUuids: Set<UUID>): Map<UUID, Result<PlayerStats>> {
        return playerUuids.associateWith { uuid -> loadStatistics(uuid) }
    }

    override suspend fun loadAllStatistics(players: Map<UUID, String>): Map<UUID, Result<PlayerStats>> {
        return players.map { (uuid, name) ->
            uuid to loadStatistics(uuid, name)
        }.toMap()
    }

    override fun getStatsFilePath(playerUuid: UUID): Path {
        return Path.of("mock/stats/$playerUuid.json")
    }

    override suspend fun statsExist(playerUuid: UUID): Boolean {
        return statsMap.containsKey(playerUuid)
    }
}
