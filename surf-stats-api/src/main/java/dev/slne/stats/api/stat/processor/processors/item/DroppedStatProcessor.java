package dev.slne.stats.api.stat.processor.processors.item;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.ItemStat;
import dev.slne.stats.api.stat.processor.StatProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DroppedStatProcessor extends StatProcessor<ItemStat> {

	@Override
	public List<ItemStat> processStats(StatPlayer player, Map<String, Long> statMap) {
		List<ItemStat> stats = new ArrayList<>();

		statMap.forEach((statName, statValue) -> {
			player.getItemStat(statName).ifPresentOrElse(
				stat -> {
					putIfLarger(stat.timesDropped(), statValue, stat::timesDropped);
					stats.add(stat);
				},
				() -> {
					ItemStat itemStat =
						new ItemStat(player.uuid(), StatsApi.getServer(), statName, 0L, 0L, 0L, 0L, 0L, statValue);

					stats.add(itemStat);
					player.addItemStat(itemStat);
				}
			);
		});

		return stats;
	}
}
