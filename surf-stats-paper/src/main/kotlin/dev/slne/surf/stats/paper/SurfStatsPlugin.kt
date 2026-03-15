package dev.slne.surf.stats.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.stats.api.StatsInstance
import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.core.service.StatsFileService
import dev.slne.surf.stats.paper.listener.PlayerStatsListener
import dev.slne.surf.surfapi.bukkit.api.event.register
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(SurfStatsPlugin::class.java)

/**
 * Main plugin class for SurfStats.
 * Handles lifecycle management and service registration.
 */
class SurfStatsPlugin : SuspendingJavaPlugin() {
    private val log = logger()

    override suspend fun onLoadAsync() {
        StatsInstance.onLoad()
    }

    override suspend fun onEnableAsync() {
        StatsInstance.onEnable()

        initializeServices()
        registerListeners()
    }

    override suspend fun onDisableAsync() {
        val onlinePlayers = server.onlinePlayers.associate { it.uniqueId to it.name }
        log.atWarning().log("Processing final stats for ${onlinePlayers.size} players on server shutdown")

        if (onlinePlayers.isNotEmpty()) {
            SurfStatsApi.processAllPlayerStats(onlinePlayers)
        }

        StatsInstance.onDisable()
    }

    private suspend fun initializeServices() {
        // Get the main world's stats directory (world/stats/)
        val mainWorld = server.worlds.firstOrNull()
            ?: throw IllegalStateException("No worlds loaded - cannot initialize stats service")

        val statsDirectory = mainWorld.worldFolder.toPath().resolve("stats")
        log.atInfo().log("Stats directory: $statsDirectory")

        // Debug Server Info
        log.atInfo().log(
            "Server name: ${StatsInstance.serverName}, display name: ${StatsInstance.serverDisplayName}"
        )

        // Initialize file service
        StatsFileService.initialize(statsDirectory)

        log.atInfo().log("Services initialized and registered")
    }

    private fun registerListeners() {
        PlayerStatsListener.register()

    }
}
