package dev.slne.stats.api.stat.processor;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The type Stat processor.
 *
 * @param <T> the type parameter
 */
public abstract class StatProcessor<T> {

	/**
	 * Process stats list.
	 *
	 * @param player  the player
	 * @param statMap the stat map
	 *
	 * @return the list
	 */
	public abstract List<T> processStats(StatPlayer player, Map<String, Long> statMap);
	
	/**
	 * Process stats list.
	 *
	 * @param uuid    the uuid
	 * @param statMap the stat map
	 *
	 * @return the list
	 */
	public List<T> processStats(UUID uuid, Map<String, Long> statMap) {
		return processStats(StatsApi.getStatPlayer(uuid), statMap);
	}

	/**
	 * Put if larger.
	 *
	 * @param oldValue the old value
	 * @param newValue the new value
	 * @param consumer the consumer
	 */
	protected void putIfLarger(long oldValue, long newValue, Consumer<Long> consumer) {
		if (newValue > oldValue) {
			consumer.accept(newValue);
		}
	}

}
