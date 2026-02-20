package dev.slne.surf.stats.core.service

import com.google.auto.service.AutoService
import dev.slne.surf.stats.api.model.PlayerStats
import dev.slne.surf.stats.api.service.StatsFileService
import dev.slne.surf.stats.core.json.StatsJsonModel
import net.kyori.adventure.util.Services
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Implementation of StatsFileService that reads player statistics from JSON files.
 */
@AutoService(StatsFileService::class)
class StatsFileServiceImpl : StatsFileService, Services.Fallback {

    private val logger = LoggerFactory.getLogger(StatsFileServiceImpl::class.java)

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
                logger.warn("Stats directory does not exist: $statsDirectory")
            } else if (!Files.isDirectory(statsDirectory)) {
                throw IllegalArgumentException("Stats path is not a directory: $statsDirectory")
            }
        }
        logger.info("StatsFileService initialized with directory: $statsDirectory")
    }

    override suspend fun loadStatistics(playerUuid: UUID): Result<PlayerStats> {
        return loadStatistics(playerUuid, "Unknown")
    }

    override suspend fun loadStatistics(playerUuid: UUID, playerName: String): Result<PlayerStats> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val filePath = getStatsFilePath(playerUuid)

                if (!filePath.exists()) {
                    logger.debug("Stats file not found for player: $playerUuid")
                    throw NoSuchElementException("Stats file not found for player: $playerUuid")
                }

                val content = filePath.readText()
                val statsModel = json.decodeFromString<StatsJsonModel>(content)

                val playerStats = PlayerStats(
                    uuid = playerUuid,
                    name = playerName,
                    dataVersion = statsModel.dataVersion,
                    stats = statsModel.toStatEntries()
                )

                logger.debug(
                    "Loaded stats for player {} ({}): {} entries, {} categories",
                    playerName, playerUuid, playerStats.stats.size, playerStats.categories().size
                )
                playerStats
            }.onFailure { error ->
                logger.error("Failed to load stats for player {}: {}", playerUuid, error.message)
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

    override suspend fun loadAllStatistics(players: Map<UUID, String>): Map<UUID, Result<PlayerStats>> {
        return coroutineScope {
            players.map { (uuid, name) ->
                async {
                    uuid to loadStatistics(uuid, name)
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
