package dev.slne.surf.stats.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.stats.api.service.fileService
import dev.slne.surf.stats.core.database.StatsDatabaseService
import dev.slne.surf.stats.paper.listener.PlayerStatsListener
import dev.slne.surf.stats.paper.listener.ServerShutdownListener
import dev.slne.surf.stats.paper.listener.WorldSaveListener
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.LoggerFactory

val plugin get() = JavaPlugin.getPlugin(SurfStatsPlugin::class.java)

/**
 * Main plugin class for SurfStats.
 * Handles lifecycle management and service registration.
 */
class SurfStatsPlugin : SuspendingJavaPlugin() {

    private val pluginLogger = LoggerFactory.getLogger(SurfStatsPlugin::class.java)

    lateinit var serverName: String

    lateinit var databaseApi: DatabaseApi
    lateinit var databaseService: StatsDatabaseService

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
        serverName = config.getString("server.name", "unknown")!!
        val serverLabel = config.getString("server.label", serverName)!!
        if (serverName == "my-server") {
            throw IllegalStateException("Server name is still set to the default value 'my-server'. Please update 'server.name' in config.yml.")
        }
        pluginLogger.info("Server name: {}, label: {}", serverName, serverLabel)

        // Initialize file service
        runBlocking {
            fileService.initialize(statsDirectory)
        }

        // Initialize database
        saveResource("database.yml", false)
        databaseApi = DatabaseApi.create(pluginPath = dataFolder.toPath())
        databaseService = StatsDatabaseService(serverName, serverLabel)

        runBlocking {
            databaseService.registerServer()
        }

        pluginLogger.info("Services initialized and registered")
    }

    private fun registerListeners() {
        val pluginManager = server.pluginManager

        pluginManager.registerEvents(
            PlayerStatsListener(),
            plugin
        )

        pluginManager.registerEvents(
            ServerShutdownListener(server),
            plugin
        )

        pluginManager.registerEvents(
            WorldSaveListener(server),
            plugin
        )

        pluginLogger.info("Event listeners registered")
    }
}
