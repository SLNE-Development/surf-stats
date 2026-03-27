package dev.slne.surf.stats.microservice

import com.google.auto.service.AutoService
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.rabbitmq.api.ServerRabbitMQApi
import dev.slne.surf.stats.microservice.handler.OptOutPacketHandler
import dev.slne.surf.stats.microservice.handler.PlayerPacketHandler
import dev.slne.surf.stats.microservice.handler.ServerPacketHandler
import dev.slne.surf.stats.microservice.handler.StatsPacketHandler
import kotlin.io.path.Path

@AutoService(Microservice::class)
class StatsMicroservice : Microservice() {
    private val dataPath = Path("config")

    private val databaseApi = DatabaseApi.create(dataPath)
    private val rabbitApi = ServerRabbitMQApi.create("surf-stats", dataPath)

    override suspend fun onBootstrap(args: List<String>) {
        rabbitApi.registerRequestHandler(StatsPacketHandler)
        rabbitApi.registerRequestHandler(ServerPacketHandler)
        rabbitApi.registerRequestHandler(PlayerPacketHandler)
        rabbitApi.registerRequestHandler(OptOutPacketHandler)
        rabbitApi.freezeAndConnect()
    }

    override suspend fun onDisable() {
        rabbitApi.disconnect()
        databaseApi.shutdown()
    }
}