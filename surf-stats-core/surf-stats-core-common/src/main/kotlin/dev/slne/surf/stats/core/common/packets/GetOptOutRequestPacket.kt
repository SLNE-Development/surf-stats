package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import kotlinx.serialization.Serializable

@Serializable
data class GetOptOutRequestPacket(
    val playerUuid: SerializableUUID
) : RabbitRequestPacket<GetOptOutResponsePacket>()