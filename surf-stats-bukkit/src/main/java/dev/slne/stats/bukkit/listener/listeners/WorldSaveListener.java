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
import org.jetbrains.annotations.ApiStatus;

import java.io.FileNotFoundException;
import java.util.UUID;

/**
 * The WorldSaveListener class is responsible for handling world save events.
 * It listens for the WorldSaveEvent and performs necessary operations when the primary world is saved.
 */
@ApiStatus.Internal
public final class WorldSaveListener implements Listener {

	/**
	 * Called when the primary world is saved.
	 * Performs necessary operations on player statistics files.
	 *
	 * @param event The WorldSaveEvent representing the save event.
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
				if (statPlayer.isDisconnected()) {
					StatsApi.getInstance().getStatPlayerManager().removeStatPlayer(statPlayer);
				}
			}).exceptionally(throwable -> {
				throw new RuntimeException(throwable);
			});
		}
	}

}
