package dev.slne.surf.stats.microservice.handler

import dev.slne.surf.rabbitmq.api.handler.RabbitHandler
import dev.slne.surf.rabbitmq.api.packet.standard.response.primitive.PrimitiveResponse
import dev.slne.surf.stats.core.common.packets.SavePlayerRequestPacket
import dev.slne.surf.stats.microservice.db.StatsDatabaseService
import kotlinx.coroutines.launch

object PlayerPacketHandler {
    @RabbitHandler
    fun handleSavePlayerRequest(request: SavePlayerRequestPacket) = request.launch {
        StatsDatabaseService.ensurePlayer(request.playerUuid, request.playerName)

        request.respond(PrimitiveResponse.BooleanResponsePacket(true))
    }
}