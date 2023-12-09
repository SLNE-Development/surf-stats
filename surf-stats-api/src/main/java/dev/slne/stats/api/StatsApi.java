package dev.slne.stats.api;

import dev.slne.data.api.DataApi;
import dev.slne.stats.api.instance.StatsInstance;
import dev.slne.stats.api.player.StatPlayer;

import java.util.UUID;

/**
 * The type Stats api.
 */
public class StatsApi {

	private static StatsInstance instance;

	/**
	 * Instantiates a new Stats api.
	 *
	 * @param instance the instance
	 */
	public StatsApi(StatsInstance instance) {
		if (StatsApi.instance != null) {
			throw new IllegalStateException("StatsApi instance already exists");
		}

		StatsApi.instance = instance;
	}

	/**
	 * Gets instance.
	 *
	 * @return the instance
	 */
	public static StatsInstance getInstance() {
		return instance;
	}

	/**
	 * Gets stat player.
	 *
	 * @param uuid the uuid
	 *
	 * @return the stat player
	 */
	public static StatPlayer getStatPlayer(UUID uuid) {
		return instance.getStatPlayer(uuid);
	}

	/**
	 * Gets server.
	 *
	 * @return the server
	 */
	public static String getServer() {
		return DataApi.getDataInstance().getServerName();
	}
}
