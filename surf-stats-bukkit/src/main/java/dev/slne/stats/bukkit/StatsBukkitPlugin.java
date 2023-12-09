package dev.slne.stats.bukkit;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.bukkit.instance.BukkitStatsInstance;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The type Stats bukkit plugin.
 */
public class StatsBukkitPlugin extends JavaPlugin {

	private static StatsBukkitPlugin instance;

	private StatsApi statsApi;
	private BukkitStatsInstance statsInstance;

	/**
	 * Gets instance.
	 *
	 * @return the instance
	 */
	public static StatsBukkitPlugin getInstance() {
		return instance;
	}

	@Override
	public void onLoad() {
		instance = this;

		statsInstance = new BukkitStatsInstance(getClassLoader());
		statsApi = new StatsApi(statsInstance);

		statsInstance.onLoad();
	}

	@Override
	public void onEnable() {
		statsInstance.onEnable();
	}


	@Override
	public void onDisable() {
		statsInstance.onDisable();
	}

	/**
	 * Stats api stats api.
	 *
	 * @return the stats api
	 */
	public StatsApi statsApi() {
		return statsApi;
	}

	/**
	 * Stats instance bukkit stats instance.
	 *
	 * @return the bukkit stats instance
	 */
	public BukkitStatsInstance statsInstance() {
		return statsInstance;
	}
}
