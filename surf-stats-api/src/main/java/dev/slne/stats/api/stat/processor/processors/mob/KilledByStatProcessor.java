package dev.slne.stats.api.stat.processor.processors.mob;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.MobStat;
import dev.slne.stats.api.stat.processor.StatProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KilledByStatProcessor extends StatProcessor<MobStat> {

	@Override
	public List<MobStat> processStats(StatPlayer player, Map<String, Long> statMap) {
		List<MobStat> stats = new ArrayList<>();

		statMap.forEach((statName, statValue) -> {
			player.getMobStat(statName).ifPresentOrElse(
				stat -> {
					putIfLarger(stat.killedBy(), statValue, stat::killedBy);
					stats.add(stat);
				},
				() -> {
					MobStat mobStat = new MobStat(player.uuid(), StatsApi.getServer(), statName, 0L, statValue);

					stats.add(mobStat);
					player.addMobStat(mobStat);
				}
			);
		});

		return stats;
	}
}
