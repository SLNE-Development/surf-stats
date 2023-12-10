package dev.slne.stats.bukkit.listener.listeners;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.bukkit.stat.PlayerStatSaver;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * The StatPlayerListener class is responsible for handling events related to player stats.
 */
@ApiStatus.Internal
public final class StatPlayerListener implements Listener {

	/**
	 * This method is an event handler for the PlayerJoinEvent.
	 * It is responsible for loading the stats for a player when they join the server.
	 *
	 * @param event The PlayerJoinEvent object
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
	 * Handles the event when a player quits the server.
	 *
	 * @param event The PlayerQuitEvent triggered when a player quits.
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
