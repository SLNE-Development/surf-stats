package dev.slne.surf.stats.core.common.packets

import dev.slne.surf.rabbitmq.api.packet.RabbitRequestPacket
import dev.slne.surf.stats.api.model.PlayerAdvancements
import kotlinx.serialization.Serializable

/**
 * Ships complete advancement snapshots to the microservice.
 *
 * Each entry replaces everything stored for its `(playerUuid, serverName)`.
 * The response carries the UUIDs of players whose snapshot failed to persist.
 */
@Serializable
data class SaveAdvancementsRequestPacket(
    val players: List<PlayerAdvancements>
) : RabbitRequestPacket<StatsUuidResponsePacket>()
