package dev.slne.surf.stats.paper.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.stats.api.surfStatsApi
import dev.slne.surf.stats.paper.plugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Server
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldSaveEvent
import java.util.UUID

class WorldSaveListener(
    private val server: Server,
) : Listener {

    // WorldSaveEvent fires once per world (overworld, nether, end), so multiple events
    // arrive in quick succession. We debounce by cancelling any pending job on each event,
    // ensuring we only process stats once per save cycle. The 5s delay also gives Minecraft
    // time to finish flushing all stats files to disk.
    private var saveJob: Job? = null

    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldSave(event: WorldSaveEvent) {
        // Capture player snapshot on the main thread (Bukkit API is not thread-safe)
        val players: Map<UUID, String> = server.onlinePlayers.associate { it.uniqueId to it.name }
        if (players.isEmpty()) return

        saveJob?.cancel()
        saveJob = plugin.launch {
            delay(5000)

            val processed = surfStatsApi.processAllPlayerStats(players)
        }
    }
}
