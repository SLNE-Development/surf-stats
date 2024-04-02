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
 * The UsedStatProcessor class is a concrete implementation of the abstract class {@link StatProcessor}.
 * It provides methods for processing and updating {@link ItemStat} objects based on a map of statistics.
 */
public final class UsedStatProcessor extends StatProcessor<ItemStat> {

	@Override
	public @NotNull List<ItemStat> processStats(StatPlayer player, @NotNull Map<String, Long> statMap) {
		List<ItemStat> stats = new ArrayList<>(statMap.size());

		statMap.forEach((statName, statValue) -> player.getItemStat(statName).ifPresentOrElse(
			stat -> {
				putIfLarger(stat.timesUsed(), statValue, stat::timesUsed);
				stats.add(stat);
			},
			() -> {
				ItemStat itemStat = ItemStat.empty(player.getUuid(), StatsApi.getServer(), statName)
											.timesUsed(statValue);

				stats.add(itemStat);
				player.addItemStat(itemStat);
			}
		));

		return stats;
	}
}
