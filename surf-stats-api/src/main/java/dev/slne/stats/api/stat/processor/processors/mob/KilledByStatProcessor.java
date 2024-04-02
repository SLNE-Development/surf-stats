package dev.slne.stats.api.stat.processor.processors.mob;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.MobStat;
import dev.slne.stats.api.stat.processor.StatProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * The KilledByStatProcessor class is a concrete implementation of the StatProcessor class for processing MobStat objects.
 * It overrides the processStats method to process the statistics of a player and update the corresponding MobStat objects.
 */
public final class KilledByStatProcessor extends StatProcessor<MobStat> {

	@Override
	public @NotNull List<MobStat> processStats(StatPlayer player, @NotNull Map<String, Long> statMap) {
		List<MobStat> stats = new ArrayList<>(statMap.size());

		statMap.forEach((statName, statValue) -> player.getMobStat(statName).ifPresentOrElse(
			stat -> {
				putIfLarger(stat.getKilleyBy(), statValue, stat::setKilledBy);
				stats.add(stat);
			},
			() -> {
				MobStat mobStat = new MobStat(player.getUuid(), StatsApi.getServer(), statName, 0L, statValue);

				stats.add(mobStat);
				player.addMobStat(mobStat);
			}
		));

		return stats;
	}
}
