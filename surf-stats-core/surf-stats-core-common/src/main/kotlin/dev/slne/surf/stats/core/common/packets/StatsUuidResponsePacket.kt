package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import dev.slne.surf.rabbitmq.api.packet.RabbitResponsePacket
import kotlinx.serialization.Serializable

@Serializable
data class StatsUuidResponsePacket(
    val value: List<SerializableUUID>
) : RabbitResponsePacket()
