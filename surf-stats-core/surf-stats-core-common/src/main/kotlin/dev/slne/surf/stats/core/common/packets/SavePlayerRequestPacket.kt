package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import dev.slne.surf.rabbitmq.api.packet.standard.response.primitive.PrimitiveResponse
import kotlinx.serialization.Serializable

@Serializable
data class SavePlayerRequestPacket(
    val playerUuid: SerializableUUID,
    val playerName: String
) : RabbitRequestPacket<PrimitiveResponse.BooleanResponsePacket>()