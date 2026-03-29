package dev.slne.surf.stats.microservice.handler

import dev.slne.surf.rabbitmq.api.handler.RabbitHandler
import dev.slne.surf.stats.core.common.packets.SaveStatsRequestPacket
import dev.slne.surf.stats.core.common.packets.StatsUuidResponsePacket
import dev.slne.surf.stats.microservice.db.StatsDatabaseService
import kotlinx.coroutines.launch
import java.util.*

object StatsPacketHandler {
    @RabbitHandler
    fun handleSaveStatsRequest(request: SaveStatsRequestPacket) = request.launch {
        val type = request.type
        val failed = when (type) {
            SaveStatsRequestPacket.Type.CURRENT -> handleCurrentSave(request)
            SaveStatsRequestPacket.Type.DIFFERENCE -> handleDifferenceSave(request)
        }.toList()

        request.respond(StatsUuidResponsePacket(failed))
    }

    private suspend fun handleCurrentSave(request: SaveStatsRequestPacket): Set<UUID> {
        return StatsDatabaseService.saveBatches(batches = request.batches)
    }

    private suspend fun handleDifferenceSave(request: SaveStatsRequestPacket): Set<UUID> {
        return StatsDatabaseService.saveDiffBatches(request.batches)
    }
}