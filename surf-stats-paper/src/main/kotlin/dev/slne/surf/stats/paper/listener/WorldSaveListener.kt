package dev.slne.surf.stats.paper.listener

import dev.slne.surf.stats.api.SurfStatsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Server
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldSaveEvent
import org.slf4j.LoggerFactory
import java.util.UUID

class WorldSaveListener(
    private val surfStatsApi: SurfStatsApi,
    private val server: Server,
    private val scope: CoroutineScope
) : Listener {

    private val logger = LoggerFactory.getLogger(WorldSaveListener::class.java)

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
        saveJob = scope.launch {
            delay(5000)

            logger.info("Processing stats for {} online players after world save", players.size)

            val processed = surfStatsApi.processAllPlayerStats(players)

            logger.info(
                "Processed stats for {}/{} players on world save",
                processed.size, players.size
            )
        }
    }
}
