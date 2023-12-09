package dev.slne.stats.bukkit.listener.listeners;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.file.PlayerStatFile;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;

import java.io.FileNotFoundException;
import java.util.UUID;

/**
 * The type World save listener.
 */
public class WorldSaveListener implements Listener {

	/**
	 * On world save.
	 *
	 * @param event the event
	 */
	@EventHandler
	public void onWorldSave(WorldSaveEvent event) {
		World primaryWorld = Bukkit.getWorlds().get(0);

		if (!event.getWorld().equals(primaryWorld)) {
			return;
		}

		PlayerStatFile.Reader reader = StatsApi.getInstance().getPlayerStatFileReader();

		for (Player player : Bukkit.getOnlinePlayers()) {
			UUID uuid = player.getUniqueId();
			StatPlayer statPlayer = StatsApi.getStatPlayer(uuid);

			PlayerStatFile statFile;
			try {
				statFile = reader.read(uuid);
			} catch (FileNotFoundException exception) {
				throw new RuntimeException(exception);
			}

			StatsApi.getInstance().getStatProcessor().processStats(statPlayer, statFile);

			statPlayer.saveStats().thenAcceptAsync(v -> {
				if (statPlayer.disconnected()) {
					StatsApi.getInstance().getStatPlayerManager().removeStatPlayer(statPlayer);
				}
			}).exceptionally(throwable -> {
				throw new RuntimeException(throwable);
			});
		}
	}

}
