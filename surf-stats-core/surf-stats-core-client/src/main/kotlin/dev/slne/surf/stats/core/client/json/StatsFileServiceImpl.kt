package dev.slne.surf.stats.core.client.json

import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Implementation of StatsFileService that reads player statistics from JSON files.
 */
object StatsFileServiceImpl : StatsFileService {
    private val log = logger()

    private lateinit var statsDirectory: Path

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun initialize(statsDirectory: Path) {
        this.statsDirectory = statsDirectory
        withContext(Dispatchers.IO) {
            if (!Files.exists(statsDirectory)) {
                log.atWarning().log("Stats directory does not exist: $statsDirectory")
            } else if (!Files.isDirectory(statsDirectory)) {
                throw IllegalArgumentException("Stats path is not a directory: $statsDirectory")
            }
        }
    }

    override suspend fun loadStatistics(playerUuid: UUID): Result<PlayerStats> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val filePath = getStatsFilePath(playerUuid)

                if (!filePath.exists()) {
                    return@withContext Result.success(
                        PlayerStats(
                            playerUuid = playerUuid,
                            serverName = SurfCoreApi.getCurrentServerName()
                        )
                    )
                }

                val content = filePath.readText()
                val statsModel = json.decodeFromString<StatsJsonModel>(content)

                val playerStats = PlayerStats(
                    playerUuid = playerUuid,
                    serverName = SurfCoreApi.getCurrentServerName(),
                    stats = statsModel.toStatEntries()
                )

                playerStats
            }.onFailure { error ->
                log.atSevere().withCause(error).log("Failed to load stats for player $playerUuid")
            }
        }
    }

    override suspend fun loadAllStatistics(playerUuids: Set<UUID>): Map<UUID, Result<PlayerStats>> {
        return coroutineScope {
            playerUuids.map { uuid ->
                async {
                    uuid to loadStatistics(uuid)
                }
            }.awaitAll().toMap()
        }
    }

    override fun getStatsFilePath(playerUuid: UUID): Path {
        return statsDirectory.resolve("$playerUuid.json")
    }

    override suspend fun statsExist(playerUuid: UUID): Boolean {
        return withContext(Dispatchers.IO) {
            getStatsFilePath(playerUuid).exists()
        }
    }
}
