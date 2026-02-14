package dev.slne.surf.stats.paper.listener

import dev.slne.surf.stats.api.SurfStatsApi
import kotlinx.coroutines.runBlocking
import org.bukkit.Server
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.slf4j.LoggerFactory

/**
 * Listener for server shutdown events.
 * Processes all online players' stats when the plugin is disabled.
 */
class ServerShutdownListener(
    private val surfStatsApi: SurfStatsApi,
    private val server: Server
) : Listener {

    private val logger = LoggerFactory.getLogger(ServerShutdownListener::class.java)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin.name != "SurfStats") return

        val onlinePlayers = server.onlinePlayers
        if (onlinePlayers.isEmpty()) {
            logger.info("No online players to process on shutdown")
            return
        }

        logger.info("Processing stats for {} online players on shutdown", onlinePlayers.size)

        val players = onlinePlayers.associate { it.uniqueId to it.name }

        runBlocking {
            val processed = surfStatsApi.processAllPlayerStats(players)
            logger.info(
                "Processed stats for {}/{} players on shutdown",
                processed.size, onlinePlayers.size
            )
        }
    }
}
