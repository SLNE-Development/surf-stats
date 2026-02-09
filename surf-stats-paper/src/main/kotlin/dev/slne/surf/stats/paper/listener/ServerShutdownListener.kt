package dev.slne.surf.stats.paper.listener

import dev.slne.surf.stats.api.SurfStatsApi
import org.bukkit.Server
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.slf4j.LoggerFactory

/**
 * Listener for server shutdown events.
 * Note: Most shutdown processing happens in plugin onDisable().
 * This listener handles edge cases and logging.
 */
class ServerShutdownListener(
    private val surfStatsApi: SurfStatsApi,
    private val server: Server
) : Listener {

    private val logger = LoggerFactory.getLogger(ServerShutdownListener::class.java)

    /**
     * Logs when the plugin is being disabled.
     * Actual shutdown processing is done in SurfStatsPlugin.onDisable()
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin.name == "SurfStats") {
            val onlineCount = server.onlinePlayers.size
            logger.info("SurfStats plugin disabling with {} players online", onlineCount)
        }
    }
}
