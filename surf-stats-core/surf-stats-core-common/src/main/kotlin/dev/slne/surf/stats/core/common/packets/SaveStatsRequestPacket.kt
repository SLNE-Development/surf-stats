package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import dev.slne.surf.stats.api.model.PlayerStatsBatch
import kotlinx.serialization.Serializable

@Serializable
data class SaveStatsRequestPacket(
    val batches: List<PlayerStatsBatch>,
    val type: Type,
) : RabbitRequestPacket<StatsUuidResponsePacket>() {
    @Serializable
    enum class Type {
        CURRENT,
        DIFFERENCE
    }
}