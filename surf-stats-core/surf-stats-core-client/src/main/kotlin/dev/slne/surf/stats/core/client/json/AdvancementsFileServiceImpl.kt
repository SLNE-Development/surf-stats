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
