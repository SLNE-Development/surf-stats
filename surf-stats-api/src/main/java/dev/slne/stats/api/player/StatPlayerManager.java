package dev.slne.stats.api.player;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Stat player manager.
 */
@ApiStatus.NonExtendable
public interface StatPlayerManager {

	/**
	 * Gets stat players.
	 *
	 * @return the stat players
	 */
	List<StatPlayer> getStatPlayers();

	/**
	 * Gets stat player.
	 *
	 * @param uuid the uuid
	 *
	 * @return the stat player
	 */
	Optional<StatPlayer> getStatPlayer(@NotNull UUID uuid);

	/**
	 * Add stat player.
	 *
	 * @param statPlayer the stat player
	 */
	@ApiStatus.Internal
	void addStatPlayer(@NotNull StatPlayer statPlayer);

	/**
	 * Remove stat player.
	 *
	 * @param statPlayer the stat player
	 */
	@ApiStatus.Internal
	void removeStatPlayer(@NotNull StatPlayer statPlayer);

	/**
	 * Remove stat player.
	 *
	 * @param uuid the uuid
	 */
	@ApiStatus.Internal
	void removeStatPlayer(@NotNull UUID uuid);

	/**
	 * Create stat player stat player.
	 *
	 * @param uuid the uuid
	 *
	 * @return the stat player
	 */
	StatPlayer createStatPlayer(@NotNull UUID uuid);

}
