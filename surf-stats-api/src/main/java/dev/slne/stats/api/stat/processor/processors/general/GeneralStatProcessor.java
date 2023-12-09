package dev.slne.stats.api.stat.processor.processors.general;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.GeneralStat;
import dev.slne.stats.api.stat.processor.StatProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type General stat processor.
 */
public class GeneralStatProcessor extends StatProcessor<GeneralStat> {

	@Override
	public List<GeneralStat> processStats(StatPlayer player, Map<String, Long> statMap) {
		List<GeneralStat> stats = new ArrayList<>();

		statMap.forEach((statName, statValue) -> {
			player.getGeneralStat(statName).ifPresentOrElse(
				stat -> {
					putIfLarger(stat.statValue(), statValue, stat::statValue);
					stats.add(stat);
				},
				() -> {
					GeneralStat generalStat = new GeneralStat(player.uuid(), StatsApi.getServer(), statName, statValue);

					stats.add(generalStat);
					player.addGeneralStat(generalStat);
				}
			);
		});

		return stats;
	}
}
