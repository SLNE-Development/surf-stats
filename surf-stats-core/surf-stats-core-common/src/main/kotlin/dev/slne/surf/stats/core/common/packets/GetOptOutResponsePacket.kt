package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.rabbitmq.api.packet.RabbitResponsePacket
import dev.slne.surf.stats.api.model.OptOutInfo
import kotlinx.serialization.Serializable

@Serializable
data class GetOptOutResponsePacket(
    val value: List<OptOutInfo>
) : RabbitResponsePacket()