package dev.slne.surf.stats.paper

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.stats.api.SurfStatsApi
import dev.slne.surf.stats.api.service.StatsFileService
import dev.slne.surf.stats.core.SurfStatsApiImpl
import dev.slne.surf.stats.core.database.StatsDatabaseService
import dev.slne.surf.stats.core.repository.PlayerStatsRepositoryImpl
import dev.slne.surf.stats.core.service.StatsFileServiceImpl
import dev.slne.surf.stats.paper.listener.PlayerStatsListener
import dev.slne.surf.stats.paper.listener.ServerShutdownListener
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.LoggerFactory

/**
 * Main plugin class for SurfStats.
 * Handles lifecycle management and service registration.
 */
class SurfStatsPlugin : JavaPlugin() {

    private val pluginLogger = LoggerFactory.getLogger(SurfStatsPlugin::class.java)

    private lateinit var fileService: StatsFileService
    private lateinit var surfStatsApi: SurfStatsApi
    private lateinit var databaseApi: DatabaseApi

    override fun onEnable() {
        pluginLogger.info("Enabling SurfStats plugin...")

        try {
            initializeServices()
            registerListeners()
            pluginLogger.info("SurfStats plugin enabled successfully")
        } catch (e: Exception) {
            pluginLogger.error("Failed to enable SurfStats plugin", e)
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        pluginLogger.info("Disabling SurfStats plugin...")

        // Shutdown database
        if (::databaseApi.isInitialized) {
            databaseApi.shutdown()
        }

        // Unregister services
        server.servicesManager.unregisterAll(this)

        pluginLogger.info("SurfStats plugin disabled")
    }

    private fun initializeServices() {
        // Get the main world's stats directory (world/stats/)
        val mainWorld = server.worlds.firstOrNull()
            ?: throw IllegalStateException("No worlds loaded - cannot initialize stats service")

        val statsDirectory = mainWorld.worldFolder.toPath().resolve("stats")
        pluginLogger.info("Using stats directory: {}", statsDirectory)

        // Determine server name from server.properties or use folder name
        val serverName = resolveServerName()
        pluginLogger.info("Server name: {}", serverName)

        // Initialize file service
        fileService = StatsFileServiceImpl()
        runBlocking {
            fileService.initialize(statsDirectory)
        }

        // Initialize database
        saveResource("database.yml", false)
        databaseApi = DatabaseApi.create(dataFolder.toPath(), "surf-stats")
        val databaseService = StatsDatabaseService()

        // Create repository and API implementation
        val repository = PlayerStatsRepositoryImpl(fileService)
        surfStatsApi = SurfStatsApiImpl(repository, serverName, databaseService)

        // Register services using Bukkit's ServicesManager
        server.servicesManager.register(
            StatsFileService::class.java,
            fileService,
            this,
            ServicePriority.Normal
        )
        server.servicesManager.register(
            SurfStatsApi::class.java,
            surfStatsApi,
            this,
            ServicePriority.Normal
        )

        pluginLogger.info("Services initialized and registered")
    }

    private fun resolveServerName(): String {
        // Try to get from server properties, fallback to world folder name
        return System.getProperty("surf.stats.server.name")
            ?: server.worlds.firstOrNull()?.name
            ?: "unknown"
    }

    private fun registerListeners() {
        val pluginManager = server.pluginManager

        pluginManager.registerEvents(
            PlayerStatsListener(surfStatsApi),
            this
        )

        pluginManager.registerEvents(
            ServerShutdownListener(surfStatsApi, server),
            this
        )

        pluginLogger.info("Event listeners registered")
    }
}
