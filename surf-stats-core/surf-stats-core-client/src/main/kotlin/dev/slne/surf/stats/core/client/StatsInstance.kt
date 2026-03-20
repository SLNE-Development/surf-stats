package dev.slne.surf.stats.core.client

import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.rabbitmq.api.ClientRabbitMQApi
import dev.slne.surf.stats.core.common.packets.SaveServerRequestPacket
import dev.slne.surf.surfapi.core.api.util.requiredService
import java.nio.file.Path

val statsInstance = requiredService<StatsInstance>()

abstract class StatsInstance {
    abstract val dataPath: Path

    val rabbitApi: ClientRabbitMQApi = ClientRabbitMQApi.create("surf-stats", dataPath)

    suspend fun onLoad() {
        println("Before connect")
        rabbitApi.freezeAndConnect()
        println("After connect")

        rabbitApi.sendRequest(
            SaveServerRequestPacket(
                serverName = SurfCoreApi.getCurrentServerName(),
                serverLabel = SurfCoreApi.getCurrentServerDisplayName()
            )
        )

        println("After send")
    }

    suspend fun onEnable() {

    }

    suspend fun onDisable() {
        rabbitApi.disconnect()
    }
}