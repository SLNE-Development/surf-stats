package dev.slne.stats.core.player;

import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.player.StatPlayerManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The type Core stat player manager.
 */
public class CoreStatPlayerManager implements StatPlayerManager {

	private final List<StatPlayer> statPlayers;

	/**
	 * Instantiates a new Core stat player manager.
	 */
	public CoreStatPlayerManager() {
		this.statPlayers = new ArrayList<>();
	}

	@Override
	public List<StatPlayer> getStatPlayers() {
		return statPlayers;
	}

	@Override
	public Optional<StatPlayer> getStatPlayer(@NotNull UUID uuid) {
		return statPlayers.stream().filter(statPlayer -> statPlayer.uuid().equals(uuid)).findFirst();
	}

	@Override
	public void addStatPlayer(@NotNull StatPlayer statPlayer) {
		this.statPlayers.add(statPlayer);
	}

	@Override
	public void removeStatPlayer(@NotNull StatPlayer statPlayer) {
		this.statPlayers.remove(statPlayer);
	}

	@Override
	public void removeStatPlayer(@NotNull UUID uuid) {
		this.statPlayers.removeIf(statPlayer -> statPlayer.uuid().equals(uuid));
	}

	@Override
	public StatPlayer createStatPlayer(@NotNull UUID uuid) {
		StatPlayer statPlayer = new StatPlayer(uuid);
		
		this.addStatPlayer(statPlayer);

		return statPlayer;
	}
}
