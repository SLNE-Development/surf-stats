package dev.slne.surf.stats.core.client

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.rabbitmq.api.ClientRabbitMQApi
import dev.slne.surf.stats.core.common.packets.SaveServerRequestPacket
import java.nio.file.Path

val statsInstance = requiredService<StatsInstance>()

abstract class StatsInstance {
    abstract val dataPath: Path

    val rabbitApi: ClientRabbitMQApi = ClientRabbitMQApi.create("surf-stats", dataPath)

    suspend fun onLoad() {
        rabbitApi.freezeAndConnect()

        rabbitApi.sendRequest(
            SaveServerRequestPacket(
                serverName = SurfCoreApi.getCurrentServerName(),
                serverLabel = SurfCoreApi.getCurrentServerDisplayName()
            )
        )
    }

    suspend fun onEnable() {

    }

    suspend fun onDisable() {
        rabbitApi.disconnect()
    }
}