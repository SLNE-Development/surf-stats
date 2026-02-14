package dev.slne.surf.stats.paper.listener

import dev.slne.surf.stats.api.SurfStatsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bukkit.Server
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldSaveEvent
import org.slf4j.LoggerFactory

class WorldSaveListener(
    private val surfStatsApi: SurfStatsApi,
    private val server: Server
) : Listener {

    private val logger = LoggerFactory.getLogger(WorldSaveListener::class.java)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldSave(event: WorldSaveEvent) {
        val onlinePlayers = server.onlinePlayers
        if (onlinePlayers.isEmpty()) return

        logger.info("World save detected, processing stats for {} online players", onlinePlayers.size)

        val players = onlinePlayers.associate { it.uniqueId to it.name }

        scope.launch {
            val processed = surfStatsApi.processAllPlayerStats(players)
            logger.info(
                "Processed stats for {}/{} players on world save",
                processed.size, onlinePlayers.size
            )
        }
    }
}
