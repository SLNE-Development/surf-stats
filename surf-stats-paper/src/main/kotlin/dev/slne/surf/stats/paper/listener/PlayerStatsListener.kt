package dev.slne.surf.stats.paper.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.stats.api.surfStatsApi
import dev.slne.surf.stats.paper.plugin
import kotlinx.coroutines.delay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener for player-related statistics events.
 */
class PlayerStatsListener() : Listener {

    /**
     * Handles player logout - processes their statistics.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId
        val name = player.name

        plugin.launch {
            // Wait for Minecraft to flush the stats JSON file to disk before reading it
            delay(1000)
            surfStatsApi.processPlayerStats(uuid, name)
        }
    }
}
