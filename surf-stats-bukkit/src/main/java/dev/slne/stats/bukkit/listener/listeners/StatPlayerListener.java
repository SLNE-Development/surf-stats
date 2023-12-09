package dev.slne.stats.bukkit.listener.listeners;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.bukkit.stat.PlayerStatSaver;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * The type Stat player listener.
 */
public class StatPlayerListener implements Listener {

	/**
	 * On player join.
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		StatsApi.getStatPlayer(event.getPlayer().getUniqueId()).loadStats().exceptionally(throwable -> {
			ComponentLogger.logger("error")
				.error("Failed to load stats for player " + event.getPlayer().getName(), throwable);

			return null;
		});
	}

	/**
	 * On player quit.
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		StatPlayer statPlayer = StatsApi.getStatPlayer(event.getPlayer().getUniqueId());
		statPlayer.disconnected(true);

		StatsApi.getInstance().getStatProcessor().processStats(statPlayer,
			PlayerStatSaver.getPlayerStatsJson(event.getPlayer()));

		statPlayer.saveStats().thenAcceptAsync(v -> {
			if (statPlayer.saving()) {
				return;
			}

			StatsApi.getInstance().getStatPlayerManager().removeStatPlayer(event.getPlayer().getUniqueId());
		});
	}
}
