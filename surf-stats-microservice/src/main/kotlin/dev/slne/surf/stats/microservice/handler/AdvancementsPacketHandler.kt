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
