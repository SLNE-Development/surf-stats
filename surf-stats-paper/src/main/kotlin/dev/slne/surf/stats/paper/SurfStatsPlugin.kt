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
import dev.slne.surf.stats.paper.listener.WorldSaveListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private val pluginScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

        // Shutdown database (PluginDisableEvent has already fired and completed the final save)
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

        // Load config
        saveDefaultConfig()
        val serverName = config.getString("server.name", "unknown")!!
        val serverLabel = config.getString("server.label", serverName)!!
        if (serverName == "my-server") {
            throw IllegalStateException("Server name is still set to the default value 'my-server'. Please update 'server.name' in config.yml.")
        }
        pluginLogger.info("Server name: {}, label: {}", serverName, serverLabel)

        // Initialize file service
        fileService = StatsFileServiceImpl()
        runBlocking {
            fileService.initialize(statsDirectory)
        }

        // Initialize database
        saveResource("database.yml", false)
        databaseApi = DatabaseApi.create(pluginPath = dataFolder.toPath())
        val databaseService = StatsDatabaseService(serverName, serverLabel)
        runBlocking {
            databaseService.registerServer()
        }

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

    private fun registerListeners() {
        val pluginManager = server.pluginManager

        pluginManager.registerEvents(
            PlayerStatsListener(surfStatsApi, pluginScope),
            this
        )

        pluginManager.registerEvents(
            ServerShutdownListener(surfStatsApi, server, pluginScope),
            this
        )

        pluginManager.registerEvents(
            WorldSaveListener(surfStatsApi, server, pluginScope),
            this
        )

        pluginLogger.info("Event listeners registered")
    }
}
