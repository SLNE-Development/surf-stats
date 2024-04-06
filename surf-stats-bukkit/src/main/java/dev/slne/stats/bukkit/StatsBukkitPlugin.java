package dev.slne.stats.bukkit;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.bukkit.instance.BukkitStatsInstance;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * The StatsBukkitPlugin class is a Bukkit plugin class that integrates the Stats API into a Bukkit server.
 */
public class StatsBukkitPlugin extends JavaPlugin {

	private StatsApi statsApi;
	private BukkitStatsInstance statsInstance;

	/**
	 * Retrieves the singleton instance of StatsBukkitPlugin.
	 *
	 * @return The singleton instance of StatsBukkitPlugin.
	 */
	public static @NotNull StatsBukkitPlugin getInstance() {
		return getPlugin(StatsBukkitPlugin.class);
	}

	@Override
	public void onLoad() {
		statsInstance = new BukkitStatsInstance(getClassLoader());
		statsApi = new StatsApi(statsInstance);

		statsInstance.onLoad();
	}

	@Override
	public void onDisable() {
		statsInstance.onDisable();
	}

	@Override
	public void onEnable() {
		statsInstance.onEnable();

		new SaveCommand(getCommand("savestats"));
	}

	/**
	 * Retrieves the StatsApi instance, which provides access to statistics and server information.
	 *
	 * @return The StatsApi instance.
	 */
	public StatsApi statsApi() {
		return statsApi;
	}

	/**
	 * Retrieves the StatsBukkitPlugin's statsInstance.
	 *
	 * @return The statsInstance of the StatsBukkitPlugin.
	 */
	public BukkitStatsInstance statsInstance() {
		return statsInstance;
	}
}
