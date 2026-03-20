package dev.slne.surf.stats.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.core.client.json.StatsFileService
import dev.slne.surf.stats.core.client.service.StatisticsManagerService
import dev.slne.surf.stats.core.client.statsInstance
import dev.slne.surf.stats.paper.listener.PlayerStatsListener
import dev.slne.surf.surfapi.bukkit.api.event.register
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.time.Duration.Companion.minutes

val plugin get() = JavaPlugin.getPlugin(SurfStatsPlugin::class.java)

/**
 * Main plugin class for SurfStats.
 * Handles lifecycle management and service registration.
 */
class SurfStatsPlugin : SuspendingJavaPlugin() {
    private val log = logger()
    private var diffSaveJob: Job? = null

    override suspend fun onLoadAsync() {
        println("Before onLoad")
        statsInstance.onLoad()
        println("After onLoad")
    }

    override suspend fun onEnableAsync() {
        statsInstance.onEnable()

        initializeServices()
        registerListeners()
        startPeriodicDiffSave()
    }

    override suspend fun onDisableAsync() {
        diffSaveJob?.cancel()

        val trackedPlayers = StatisticsManagerService.snapshotMap
            .map { entry -> entry.playerUuid }.toSet()

        log.atWarning().log("Processing final stats for ${trackedPlayers.size} players on server shutdown")

        saveTrackedPlayerStats()

        statsInstance.onDisable()
    }

    private suspend fun initializeServices() {
        // Get the main world's stats directory (world/stats/)
        val mainWorld = server.worlds.firstOrNull()
            ?: throw IllegalStateException("No worlds loaded - cannot initialize stats service")

        val statsDirectory = mainWorld.worldFolder.toPath().resolve("stats")
        log.atInfo().log("Stats directory: $statsDirectory")

        // Debug Server Info
        log.atInfo().log(
            "Server name: ${SurfCoreApi.getCurrentServerName()}, display name: ${SurfCoreApi.getCurrentServerDisplayName()}"
        )

        // Initialize file service
        StatsFileService.initialize(statsDirectory)

        log.atInfo().log("Services initialized and registered")
    }

    private fun registerListeners() {
        PlayerStatsListener.register()
    }

    private fun startPeriodicDiffSave() {
        val interval = SAVE_INTERVAL

        diffSaveJob = launch {
            delay(interval)
            while (isActive) {
                runCatching {
                    saveTrackedPlayerStats()
                }.onFailure { e ->
                    log.atSevere().withCause(e).log("Failed to save periodic stat diffs")
                }
                delay(interval)
            }
        }

        log.atInfo().log("Periodic diff save started (interval: ${interval.inWholeMinutes}m)")
    }

    private suspend fun saveTrackedPlayerStats() {
        val trackedPlayers = StatisticsManagerService.snapshotMap
            .map { entry -> entry.playerUuid }.toSet()

        if (trackedPlayers.isNotEmpty()) {
            log.atInfo().log("Saving diffs for ${trackedPlayers.size} players")

            flushAllPlayerStats(trackedPlayers)
            SurfStatsApi.processAllPlayerStats(trackedPlayers)
        }
    }

    private fun flushAllPlayerStats(playerUuids: Set<UUID>) {
        playerUuids.forEach { uuid ->
            server.getPlayer(uuid)?.let { flushPlayerStats(it) }
        }
    }

    /**
     * Forces Minecraft to write the player's stats JSON file to disk.
     * Uses reflection to call CraftPlayer -> ServerPlayer -> ServerStatsCounter.save().
     */
    private fun flushPlayerStats(player: Player) {
        try {
            val handle = player.javaClass.getMethod("getHandle").invoke(player)
            val stats = handle.javaClass.getMethod("getStats").invoke(handle)
            stats.javaClass.getMethod("save").invoke(stats)
        } catch (e: Exception) {
            log.atWarning().withCause(e).log("Failed to flush stats for ${player.name}")
        }
    }

    companion object {
        private val SAVE_INTERVAL = 5.minutes
    }
}
