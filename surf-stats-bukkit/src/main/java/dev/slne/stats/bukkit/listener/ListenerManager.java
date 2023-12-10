package dev.slne.stats.bukkit.listener;

import dev.slne.stats.bukkit.StatsBukkitPlugin;
import dev.slne.stats.bukkit.listener.listeners.StatPlayerListener;
import dev.slne.stats.bukkit.listener.listeners.WorldSaveListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

/**
 * The ListenerManager class is responsible for managing listeners and registering them with the plugin manager.
 * It provides methods for enabling and disabling the listeners, accessing the list of registered listeners,
 * and retrieving the plugin manager and java plugin associated with the listener manager.
 */
@ApiStatus.Internal
public final class ListenerManager {

	private final List<Listener> listeners;
	private PluginManager pluginManager;
	private JavaPlugin plugin;

	/**
	 * The ListenerManager class is responsible for managing listeners and registering them with the plugin manager.
	 */
	@Contract(pure = true)
	public ListenerManager() {
		this.listeners = new ArrayList<>();
	}

	/**
	 * Register listeners.
	 */
	private void registerListeners() {
		listeners.add(new WorldSaveListener());
		listeners.add(new StatPlayerListener());
	}

	/**
	 * On enable.
	 */
	public void onEnable() {
		pluginManager = Bukkit.getPluginManager();
		plugin = StatsBukkitPlugin.getInstance();

		registerListeners();

		listeners.forEach(listener -> pluginManager.registerEvents(listener, plugin));
	}

	/**
	 * Unregisters all listeners associated with the plugin.
	 */
	public void onDisable() {
		HandlerList.unregisterAll(plugin);
	}

	/**
	 * Returns the list of registered listeners.
	 *
	 * @return The list of registered listeners.
	 */
	@Contract(pure = true)
	public List<Listener> listeners() {
		return listeners;
	}

	/**
	 * Retrieves the PluginManager associated with the ListenerManager.
	 *
	 * @return The PluginManager instance associated with the ListenerManager.
	 */
	@Contract(pure = true)
	public PluginManager pluginManager() {
		return pluginManager;
	}

	/**
	 * Returns the JavaPlugin instance associated with the ListenerManager.
	 *
	 * @return The JavaPlugin instance.
	 */
	@Contract(pure = true)
	public JavaPlugin plugin() {
		return plugin;
	}
}
