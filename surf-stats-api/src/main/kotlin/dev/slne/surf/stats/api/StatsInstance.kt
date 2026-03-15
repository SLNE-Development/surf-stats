package dev.slne.surf.stats.api

import dev.slne.surf.stats.api.utils.InternalStatsApi
import dev.slne.surf.surfapi.core.api.util.requiredService
import java.nio.file.Path

private val instance = requiredService<StatsInstance>()

@InternalStatsApi
interface StatsInstance {
    val serverName: String
    val serverDisplayName: String
    val dataPath: Path

    suspend fun onLoad()
    suspend fun onEnable()
    suspend fun onDisable()

    companion object : StatsInstance by instance {
        val INSTANCE get() = instance
    }
}