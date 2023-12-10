package dev.slne.stats.api;

import dev.slne.data.api.DataApi;
import dev.slne.stats.api.instance.StatsInstance;
import dev.slne.stats.api.player.StatPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

/**
 * The StatsApi class provides access to statistics and server information.
 */
@ApiStatus.NonExtendable
public final class StatsApi {

	/**
	 * Represents an instance of the Stats API.
	 */
	private static StatsInstance instance;

	/**
	 * The StatsApi class is used to create an instance of the Stats API.
	 *
	 * @param instance the instance of StatsInstance
	 */
	@ApiStatus.Internal
	public StatsApi(@NotNull StatsInstance instance) {
		checkState(StatsApi.instance == null, "StatsApi instance already exists");

		StatsApi.instance = instance;
	}

	/**
	 * Retrieves the singleton instance of StatsInstance.
	 *
	 * @return the singleton instance of StatsInstance
	 */
	@Contract(pure = true)
	public static StatsInstance getInstance() {
		return instance;
	}

	/**
	 * Retrieves the StatPlayer corresponding to the specified UUID.
	 *
	 * @param uuid the UUID of the player
	 * @return the StatPlayer object associated with the UUID, or null if the player does not exist
	 */
	public static StatPlayer getStatPlayer(UUID uuid) {
		return instance.getStatPlayer(uuid);
	}

	/**
	 * Returns the name of the server.
	 *
	 * @return the server name
	 */
	public static String getServer() {
		return DataApi.getDataInstance().getServerName();
	}
}
