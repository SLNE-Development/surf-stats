package dev.slne.stats.core.player;

import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.player.StatPlayerManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * The CoreStatPlayerManager class is an implementation of the StatPlayerManager interface.
 * It provides methods to manage a collection of StatPlayer objects.
 */
@ApiStatus.NonExtendable
@ApiStatus.Internal
public final class CoreStatPlayerManager implements StatPlayerManager {

	private final Object2ObjectMap<UUID, StatPlayer> statPlayers; // No shitty list here

	/**
	 * The CoreStatPlayerManager is a class that implements the StatPlayerManager interface.
	 * It provides methods to manage a collection of StatPlayer objects.
	 */
	public CoreStatPlayerManager() {
		this.statPlayers = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>()); // Instead, a cool fastutil map
	}

	@Contract(pure = true)
	@Override
	public @NotNull ObjectCollection<StatPlayer> getStatPlayers() {
		return statPlayers.values();
	}

	@Override
	public Optional<StatPlayer> getStatPlayer(@NotNull UUID uuid) {
		return Optional.ofNullable(statPlayers.get(uuid));
	}

	@Override
	public void addStatPlayer(@NotNull StatPlayer statPlayer) {
		this.statPlayers.put(statPlayer.uuid(), statPlayer);
	}

	@Override
	public void removeStatPlayer(@NotNull StatPlayer statPlayer) {
		this.statPlayers.remove(statPlayer.uuid());
	}

	@Override
	public void removeStatPlayer(@NotNull UUID uuid) {
		this.statPlayers.remove(uuid);
	}

	@Override
	public @NotNull StatPlayer createStatPlayer(@NotNull UUID uuid) {
		StatPlayer statPlayer = new StatPlayer(uuid);

		this.addStatPlayer(statPlayer);

		return statPlayer;
	}
}
