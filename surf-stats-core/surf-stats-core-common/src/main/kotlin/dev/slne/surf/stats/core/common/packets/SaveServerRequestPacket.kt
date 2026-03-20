package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import dev.slne.surf.rabbitmq.api.packet.standard.response.primitive.PrimitiveResponse
import kotlinx.serialization.Serializable

@Serializable
data class SaveServerRequestPacket(
    val serverName: String,
    val serverLabel: String
) : RabbitRequestPacket<PrimitiveResponse.BooleanResponsePacket>()