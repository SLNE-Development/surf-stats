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
 * This class represents a broken stat processor that extends the abstract StatProcessor class.
 * It is responsible for processing item stats related to times broken by a player.
 */
public final class BrokenStatProcessor extends StatProcessor<ItemStat> {

	@Override
	public List<ItemStat> processStats(StatPlayer player, @NotNull Map<String, Long> statMap) {
		List<ItemStat> stats = new ArrayList<>(statMap.size());

		statMap.forEach((statName, statValue) -> player.getItemStat(statName).ifPresentOrElse(
			stat -> {
				putIfLarger(stat.timesBroken(), statValue, stat::timesBroken);
				stats.add(stat);
			},
			() -> {
				ItemStat itemStat = ItemStat.empty(player.uuid(), StatsApi.getServer(), statName)
					.timesBroken(statValue);

				stats.add(itemStat);
				player.addItemStat(itemStat);
			}
		));

		return stats;
	}
}
