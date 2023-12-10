package dev.slne.stats.api.stat.processor;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The StatProcessor class is an abstract class that defines the basic structure and behaviors of a stat processor.
 * It provides methods for processing different types of stats and updating the corresponding objects.
 *
 * @param <T> The type of the stat object.
 */
public abstract class StatProcessor<T> {

	/**
	 * Process the statistics for a player.
	 *
	 * @param player   The player for whom the statistics are processed.
	 * @param statMap  A map containing the statistics to be processed, where the keys represent the stat names and the
	 *                 values represent the corresponding values.
	 * @return A list of processed stat objects.
	 */
	public abstract List<T> processStats(StatPlayer player, Map<String, Long> statMap);

	/**
	 * Process the stats for a player identified by the UUID and update the corresponding objects.
	 *
	 * @param uuid     The UUID of the player.
	 * @param statMap  A map of stat names and their corresponding values.
	 * @return A list of updated stat objects.
	 */
	public List<T> processStats(UUID uuid, Map<String, Long> statMap) {
		return processStats(StatsApi.getStatPlayer(uuid), statMap);
	}

	/**
	 * The putIfLarger method compares two long values and calls the consumer function with the larger value.
	 *
	 * @param oldValue  The old value to compare.
	 * @param newValue  The new value to compare.
	 * @param consumer  The consumer function to be called with the larger value.
	 */
	protected void putIfLarger(long oldValue, long newValue, Consumer<Long> consumer) {
		if (newValue > oldValue) {
			consumer.accept(newValue);
		}
	}
}
