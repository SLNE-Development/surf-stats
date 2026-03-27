package dev.slne.surf.stats.microservice.handler

import dev.slne.surf.rabbitmq.api.handler.RabbitHandler
import dev.slne.surf.stats.core.common.packets.GetOptOutRequestPacket
import dev.slne.surf.stats.core.common.packets.GetOptOutResponsePacket
import dev.slne.surf.stats.core.common.packets.SaveOptOutRequestPacket
import dev.slne.surf.stats.core.common.packets.SaveOptOutResponsePacket
import dev.slne.surf.stats.microservice.db.StatsDatabaseService
import kotlinx.coroutines.launch

object OptOutPacketHandler {
    @RabbitHandler
    fun handleSavePlayerOptOut(request: SaveOptOutRequestPacket) = request.launch {
        StatsDatabaseService.toggleOptOutStatistic(
            playerUuid = request.playerUuid,
            categoryName = request.categoryName,
            statisticName = request.statisticName,
            type = request.type
        )

        request.respond(
            SaveOptOutResponsePacket(
                categoryName = request.categoryName,
                statisticName = request.statisticName,
                type = request.type
            )
        )
    }

    @RabbitHandler
    fun handleGetOptOut(request: GetOptOutRequestPacket) = request.launch {
        val optOutInfoList = StatsDatabaseService.getOptOut(request.playerUuid)
        request.respond(
            GetOptOutResponsePacket(
                value = optOutInfoList
            )
        )
    }
}