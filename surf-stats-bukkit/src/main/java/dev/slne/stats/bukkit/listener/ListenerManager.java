package dev.slne.stats.bukkit.listener;

import dev.slne.stats.bukkit.StatsBukkitPlugin;
import dev.slne.stats.bukkit.listener.listeners.StatPlayerListener;
import dev.slne.stats.bukkit.listener.listeners.WorldSaveListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Listener manager.
 */
public class ListenerManager {

	private final List<Listener> listeners;
	private PluginManager pluginManager;
	private JavaPlugin plugin;

	/**
	 * Instantiates a new Listener manager.
	 */
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
	 * On disable.
	 */
	public void onDisable() {
		listeners.forEach(HandlerList::unregisterAll);
	}

	/**
	 * Listeners list.
	 *
	 * @return the list
	 */
	public List<Listener> listeners() {
		return listeners;
	}

	/**
	 * Plugin manager plugin manager.
	 *
	 * @return the plugin manager
	 */
	public PluginManager pluginManager() {
		return pluginManager;
	}

	/**
	 * Plugin java plugin.
	 *
	 * @return the java plugin
	 */
	public JavaPlugin plugin() {
		return plugin;
	}
}
