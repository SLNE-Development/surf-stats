package dev.slne.stats.api.stat.processor.processors.item;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.ItemStat;
import dev.slne.stats.api.stat.processor.StatProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The PickedUpStatProcessor class is a concrete implementation of the StatProcessor class.
 * It processes the picked-up item statistics for a player and updates the corresponding item stat objects.
 */
public final class PickedUpStatProcessor extends StatProcessor<ItemStat> {

	@Override
	public @NotNull List<ItemStat> processStats(StatPlayer player, @NotNull Map<String, Long> statMap) {
		List<ItemStat> stats = new ArrayList<>(statMap.size());

		statMap.forEach((statName, statValue) -> player.getItemStat(statName).ifPresentOrElse(
			stat -> {
				putIfLarger(stat.getTimesPickedUp(), statValue, stat::setTimesPickedUp);
				stats.add(stat);
			},
			() -> {
				ItemStat itemStat = ItemStat.empty(player.getUuid(), StatsApi.getServer(), statName)
											.setTimesPickedUp(statValue);

				stats.add(itemStat);
				player.addItemStat(itemStat);
			}
		));

		return stats;
	}
}
