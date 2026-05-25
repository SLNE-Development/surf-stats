package dev.slne.surf.stats.paper.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.stats.core.client.surfStatsApiImpl
import dev.slne.surf.stats.paper.plugin
import kotlinx.coroutines.delay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.time.Duration.Companion.milliseconds

/**
 * Listener for player join/quit events.
 * On join: loads the initial stat snapshot into memory.
 * On quit: computes final diffs, saves to DB, and removes from tracking.
 */
object PlayerStatsListener : Listener {
    /**
     * Handles player join - starts tracking their statistics.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId
        val name = player.name

        plugin.launch {
            surfStatsApiImpl.onPlayerJoin(uuid, name)
        }
    }

    /**
     * Handles player logout - computes final diffs, saves, and stops tracking.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId

        plugin.launch {
            // Wait for Minecraft to flush the stats JSON file to disk before reading it
            delay(1000.milliseconds)
            surfStatsApiImpl.onPlayerQuit(uuid)
        }
    }
}
