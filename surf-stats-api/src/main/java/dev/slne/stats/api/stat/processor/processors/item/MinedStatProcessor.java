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
 * The MinedStatProcessor class is a concrete subclass of the StatProcessor class.
 * It provides implementation for processing mined item statistics and updating the corresponding ItemStat objects.
 */
public final class MinedStatProcessor extends StatProcessor<ItemStat> {

	@Override
	public @NotNull List<ItemStat> processStats(StatPlayer player, @NotNull Map<String, Long> statMap) {
		List<ItemStat> stats = new ArrayList<>(statMap.size());

		statMap.forEach((statName, statValue) -> player.getItemStat(statName).ifPresentOrElse(
			stat -> {
				putIfLarger(stat.getTimesMined(), statValue, stat::setTimesMined);
				stats.add(stat);
			},
			() -> {
				ItemStat itemStat = ItemStat.empty(player.getUuid(), StatsApi.getServer(), statName)
											.setTimesMined(statValue);

				stats.add(itemStat);
				player.addItemStat(itemStat);
			}
		));

		return stats;
	}
}
