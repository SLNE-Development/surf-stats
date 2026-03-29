package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import dev.slne.surf.stats.api.model.OptOutType
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable

@Serializable
data class SaveOptOutRequestPacket(
    val playerUuid: SerializableUUID,
    val categoryName: String,
    val statisticName: String,
    val type: OptOutType
) : RabbitRequestPacket<SaveOptOutResponsePacket>()