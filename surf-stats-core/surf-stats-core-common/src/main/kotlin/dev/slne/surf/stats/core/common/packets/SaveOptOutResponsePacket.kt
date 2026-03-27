package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.rabbitmq.api.packet.RabbitResponsePacket
import dev.slne.surf.stats.api.model.OptOutType
import kotlinx.serialization.Serializable

@Serializable
data class SaveOptOutResponsePacket(
    val categoryName: String,
    val statisticName: String,
    val type: OptOutType,
) : RabbitResponsePacket()