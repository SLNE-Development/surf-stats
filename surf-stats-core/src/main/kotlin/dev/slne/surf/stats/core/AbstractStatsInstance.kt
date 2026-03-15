package dev.slne.surf.stats.core

import dev.slne.surf.core.api.common.surfCoreApi
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.stats.api.StatsInstance
import dev.slne.surf.stats.core.database.StatsDatabaseService

abstract class AbstractStatsInstance : StatsInstance {
    override val serverName get() = surfCoreApi.getCurrentServerName()
    override val serverDisplayName get() = surfCoreApi.getCurrentServerDisplayName()

    private lateinit var databaseApi: DatabaseApi

    override suspend fun onLoad() {
        databaseApi = DatabaseApi.create(pluginPath = dataPath)

        StatsDatabaseService.registerServer()
    }

    override suspend fun onEnable() {

    }

    override suspend fun onDisable() {
        if (::databaseApi.isInitialized) {
            databaseApi.shutdown()
        }
    }
}