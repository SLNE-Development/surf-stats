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
                .log("Failed to load advancements for %s players: %s", failed.size, failed)
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
