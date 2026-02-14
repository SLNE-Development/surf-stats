package dev.slne.surf.stats.paper.listener

import dev.slne.surf.stats.api.SurfStatsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.slf4j.LoggerFactory

/**
 * Listener for player-related statistics events.
 */
class PlayerStatsListener(
    private val surfStatsApi: SurfStatsApi
) : Listener {

    private val logger = LoggerFactory.getLogger(PlayerStatsListener::class.java)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Handles player logout - processes their statistics.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId
        val name = player.name

        logger.debug("Player {} ({}) disconnected, processing stats", name, uuid)

        scope.launch {
            // Wait for Minecraft to flush the stats JSON file to disk before reading it
            delay(1000)
            val result = surfStatsApi.processPlayerStats(uuid, name)

            result.fold(
                onSuccess = { batch ->
                    logger.info(
                        "Processed stats for {} ({}): {} entries on server '{}'",
                        name, uuid, batch.player.stats.size, batch.serverName
                    )
                },
                onFailure = { error ->
                    logger.warn("Failed to process stats for {} ({}): {}", name, uuid, error.message)
                }
            )
        }
    }
}
