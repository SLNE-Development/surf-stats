package dev.slne.stats.api.player;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * The StatPlayerManager interface represents a manager for managing StatPlayer objects.
 */
@ApiStatus.NonExtendable
public interface StatPlayerManager {

	/**
	 * Returns a list of StatPlayer objects.
	 *
	 * @return a list of StatPlayer objects
	 */
	ObjectCollection<StatPlayer> getStatPlayers();

	/**
	 * Retrieves the StatPlayer object associated with the specified UUID.
	 *
	 * @param uuid The UUID of the player.
	 * @return An Optional containing the StatPlayer object if it exists, otherwise an empty Optional.
	 */
	Optional<StatPlayer> getStatPlayer(@NotNull UUID uuid);

	/**
	 * Adds a StatPlayer object to the manager.
	 *
	 * @param statPlayer the StatPlayer object to be added
	 */
	@ApiStatus.Internal
	void addStatPlayer(@NotNull StatPlayer statPlayer);

	/**
	 * Removes the specified StatPlayer from the StatPlayerManager.
	 *
	 * @param statPlayer The StatPlayer to remove. Must not be null.
	 */
	@ApiStatus.Internal
	void removeStatPlayer(@NotNull StatPlayer statPlayer);

	/**
	 * Removes a StatPlayer from the StatPlayerManager.
	 *
	 * @param uuid the UUID of the player to remove
	 */
	@ApiStatus.Internal
	void removeStatPlayer(@NotNull UUID uuid);

	/**
	 * Creates a StatPlayer object with the specified UUID.
	 *
	 * @param uuid The UUID of the player.
	 * @return The created StatPlayer object.
	 */
	StatPlayer createStatPlayer(@NotNull UUID uuid);

}
