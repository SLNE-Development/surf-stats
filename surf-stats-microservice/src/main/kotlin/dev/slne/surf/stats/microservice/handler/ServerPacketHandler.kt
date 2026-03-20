package dev.slne.surf.stats.microservice.handler

import dev.slne.surf.rabbitmq.api.handler.RabbitHandler
import dev.slne.surf.rabbitmq.api.packet.standard.response.primitive.PrimitiveResponse
import dev.slne.surf.stats.core.common.packets.SaveServerRequestPacket
import dev.slne.surf.stats.microservice.db.StatsDatabaseService
import kotlinx.coroutines.launch

object ServerPacketHandler {
    @RabbitHandler
    fun handleSaveServerRequest(request: SaveServerRequestPacket) = request.launch {
        StatsDatabaseService.ensureServer(request.serverName, request.serverLabel)

        request.respond(PrimitiveResponse.BooleanResponsePacket(true))
    }
}