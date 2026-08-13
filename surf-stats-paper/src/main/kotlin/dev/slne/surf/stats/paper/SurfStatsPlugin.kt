package dev.slne.surf.stats.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.inventory.framework.viewFrame
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.core.client.json.AdvancementsFileService
import dev.slne.surf.stats.core.client.json.StatsFileService
import dev.slne.surf.stats.core.client.service.StatisticsManagerService
import dev.slne.surf.stats.core.client.statsInstance
import dev.slne.surf.stats.paper.commands.statsCommand
import dev.slne.surf.stats.paper.listener.PlayerStatsListener
import dev.slne.surf.stats.paper.listener.WorldSaveListener
import dev.slne.surf.stats.paper.menu.statsOptOutView
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
        statsInstance.onLoad()
        viewFrame.with(statsOptOutView)
    }

    override suspend fun onEnableAsync() {
        statsInstance.onEnable()

        initializeServices()
        registerListeners()
        startPeriodicDiffSave()

        statsCommand()
    }

    override suspend fun onDisableAsync() {
        diffSaveJob?.cancel()

        val trackedPlayers = StatisticsManagerService.trackedPlayers

        log.atWarning()
            .log("Processing final stats for ${trackedPlayers.size} players on server shutdown")

        saveTrackedPlayerStats()
        saveTrackedPlayerAdvancements()

        statsInstance.onDisable()
    }

    @NmsUseWithCaution
    private suspend fun initializeServices() {
        val statsDirectory = SurfPaperNmsPlayerBridge.getStatsDataPath()
        log.atInfo().log("Stats directory: $statsDirectory")

        // Debug Server Info
        log.atInfo().log(
            "Server name: ${SurfCoreApi.getCurrentServerName()}, display name: ${SurfCoreApi.getCurrentServerDisplayName()}"
        )

        // Initialize file service
        StatsFileService.initialize(statsDirectory)

        // stats/ and advancements/ are siblings inside the world directory; the
        // NMS bridge exposes only the stats path.
        val advancementsDirectory = statsDirectory.resolveSibling("advancements")
        log.atInfo().log("Advancements directory: $advancementsDirectory")

        AdvancementsFileService.initialize(advancementsDirectory)

        log.atInfo().log("Services initialized and registered")
    }

    private fun registerListeners() {
        PlayerStatsListener.register()
        WorldSaveListener.register()
    }

    private fun startPeriodicDiffSave() {
        val interval = SAVE_INTERVAL

        diffSaveJob = launch {
            delay(interval)
            while (isActive) {
                runCatching {
                    log.atInfo().log("Saving periodic stat diffs")
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
        val trackedPlayers = StatisticsManagerService.trackedPlayers

        if (trackedPlayers.isNotEmpty()) {
            log.atInfo().log("Saving diffs for ${trackedPlayers.size} players")

            flushAll(trackedPlayers, STATS_ACCESSOR)
            SurfStatsApi.processAllPlayerStats(trackedPlayers)
        }
    }

    /**
     * Flushes and ships advancements for every tracked player.
     *
     * Called from [WorldSaveListener] and on shutdown. Players whose snapshot is
     * unchanged are filtered out inside the API.
     */
    suspend fun saveTrackedPlayerAdvancements() {
        val trackedPlayers = StatisticsManagerService.trackedPlayers

        if (trackedPlayers.isEmpty()) {
            return
        }

        log.atInfo().log("Saving advancements for ${trackedPlayers.size} players")

        flushAll(trackedPlayers, ADVANCEMENTS_ACCESSOR)
        SurfStatsApi.processAllPlayerAdvancements(trackedPlayers)
    }

    private fun flushAll(playerUuids: Set<UUID>, accessor: String) {
        playerUuids.forEach { uuid ->
            server.getPlayer(uuid)?.let { flushViaReflection(it, accessor) }
        }
    }

    /**
     * Forces Minecraft to write one of the player's data files to disk.
     *
     * Uses reflection to call CraftPlayer -> ServerPlayer -> [accessor]() -> save(),
     * which under Paper's Mojang mappings reaches `ServerStatsCounter.save()` for
     * [STATS_ACCESSOR] and `PlayerAdvancements.save()` for [ADVANCEMENTS_ACCESSOR].
     */
    private fun flushViaReflection(player: Player, accessor: String) {
        try {
            val handle = player.javaClass.getMethod("getHandle").invoke(player)
            val target = handle.javaClass.getMethod(accessor).invoke(handle)
            target.javaClass.getMethod("save").invoke(target)
        } catch (e: Exception) {
            log.atWarning().withCause(e).log("Failed to flush $accessor for ${player.name}")
        }
    }

    companion object {
        private val SAVE_INTERVAL = 5.minutes

        private const val STATS_ACCESSOR = "getStats"
        private const val ADVANCEMENTS_ACCESSOR = "getAdvancements"
    }
}