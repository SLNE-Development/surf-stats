package dev.slne.surf.stats.core.client

import dev.slne.surf.core.api.common.surfCoreApi
import dev.slne.surf.rabbitmq.api.ClientRabbitMQApi
import dev.slne.surf.stats.core.common.packets.SaveServerRequestPacket
import dev.slne.surf.surfapi.core.api.util.requiredService
import java.nio.file.Path

val statsInstance = requiredService<StatsInstance>()

abstract class StatsInstance {
    abstract val dataPath: Path

    val rabbitApi: ClientRabbitMQApi = ClientRabbitMQApi.create("surf-stats", dataPath)

    suspend fun onLoad() {
        rabbitApi.freezeAndConnect()

        rabbitApi.sendRequest(
            SaveServerRequestPacket(
                serverName = surfCoreApi.getCurrentServerName(),
                serverLabel = surfCoreApi.getCurrentServerDisplayName()
            )
        )
    }

    suspend fun onEnable() {

    }

    suspend fun onDisable() {
        rabbitApi.disconnect()
    }
}