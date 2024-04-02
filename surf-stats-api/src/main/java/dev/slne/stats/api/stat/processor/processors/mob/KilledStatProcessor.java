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
 * The {@code KilledStatProcessor} class is a concrete implementation of the {@code StatProcessor} class that processes
 * and updates {@code MobStat} objects for a player's killed mobs.
 */
public final class KilledStatProcessor extends StatProcessor<MobStat> {

	@Override
	public @NotNull List<MobStat> processStats(StatPlayer player, @NotNull Map<String, Long> statMap) {
		List<MobStat> stats = new ArrayList<>(statMap.size());

		statMap.forEach((statName, statValue) -> player.getMobStat(statName).ifPresentOrElse(
			stat -> {
				putIfLarger(stat.killed(), statValue, stat::killed);
				stats.add(stat);
			},
			() -> {
				MobStat mobStat = new MobStat(player.getUuid(), StatsApi.getServer(), statName, statValue, 0L);

				stats.add(mobStat);
				player.addMobStat(mobStat);
			}
		));

		return stats;
	}
}
