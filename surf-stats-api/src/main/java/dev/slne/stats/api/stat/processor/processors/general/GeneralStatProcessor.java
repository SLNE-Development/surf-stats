package dev.slne.stats.api.stat.processor.processors.general;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.GeneralStat;
import dev.slne.stats.api.stat.processor.StatProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The GeneralStatProcessor class is responsible for processing general statistics for a player.
 * It extends the abstract class StatProcessor with GeneralStat as the generic type.
 */
public final class GeneralStatProcessor extends StatProcessor<GeneralStat> {

	@Override
	public @NotNull List<GeneralStat> processStats(StatPlayer player, @NotNull Map<String, Long> statMap) {
		List<GeneralStat> stats = new ArrayList<>(statMap.size());

		statMap.forEach((statName, statValue) -> player.getGeneralStat(statName).ifPresentOrElse(
			stat -> {
				putIfLarger(stat.statValue(), statValue, stat::statValue);
				stats.add(stat);
			},
			() -> {
				GeneralStat generalStat = new GeneralStat(player.getUuid(), StatsApi.getServer(), statName, statValue);

				stats.add(generalStat);
				player.addGeneralStat(generalStat);
			}
		));

		return stats;
	}
}
