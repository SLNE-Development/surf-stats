package dev.slne.stats.api.player;

import dev.slne.stats.api.stat.GeneralStat;
import dev.slne.stats.api.stat.ItemStat;
import dev.slne.stats.api.stat.MobStat;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * StatPlayer represents a player's statistics in a game. It provides methods to load and save the player's stats,
 * as well as retrieve specific stats and add new stats.
 */
public interface StatPlayer {

	/**
	 * Returns the current saving status of the StatPlayer instance.
	 *
	 * @return true if the StatPlayer instance is currently saving, false otherwise
	 */
	boolean isSaving();

	/**
	 * Saves the current state of the "saving" flag for the StatPlayer.
	 *
	 * @param saving the value indicating whether the saving flag should be enabled or disabled
	 *
	 * @return the current instance of StatPlayer
	 */
	StatPlayer setSaving(boolean saving);

	/**
	 * Determines if the player is disconnected.
	 *
	 * @return true if the player is disconnected, false otherwise.
	 */
	boolean isDisconnected();

	/**
	 * Sets the disconnected status of the player.
	 *
	 * @param disconnected the disconnected status of the player
	 *
	 * @return the modified StatPlayer object
	 */
	StatPlayer setDisconnected(boolean disconnected);

	/**
	 * Checks if the instance of StatPlayer is loaded.
	 *
	 * @return true if the StatPlayer instance is loaded; otherwise, false.
	 */
	boolean isLoaded();

	/**
	 * Sets the loaded status of the player.
	 *
	 * @param loaded true if the player is loaded, false otherwise
	 *
	 * @return the instance of the {@link StatPlayer}
	 */
	StatPlayer setLoaded(boolean loaded);

	/**
	 * Loads the statistics for the player asynchronously.
	 *
	 * @return a CompletableFuture that completes when the statistics are loaded
	 */
	CompletableFuture<Void> loadStats();

	/**
	 * Loads the general stats for the player.
	 * Clears the existing general stats and retrieves the general stats from the generalStatRepository.
	 *
	 * @return a CompletableFuture containing a list of GeneralStat objects
	 */
	CompletableFuture<List<GeneralStat>> loadGeneralStats();

	/**
	 * Loads the item statistics for the player.
	 *
	 * @return a CompletableFuture that returns a List of ItemStat objects representing the loaded item statistics
	 */
	CompletableFuture<List<ItemStat>> loadItemStats();

	/**
	 * Loads the MobStats for the player.
	 *
	 * @return a CompletableFuture that completes with a List of MobStat objects representing the loaded MobStats
	 */
	CompletableFuture<List<MobStat>> loadMobStats();

	/**
	 * Retrieves the UUID associated with the player.
	 *
	 * @return the UUID of the player
	 */
	UUID getUuid();

	/**
	 * Retrieves the list of general statistics.
	 *
	 * @return the list of general statistics
	 */
	List<GeneralStat> getGeneralStats();

	/**
	 * Retrieves the list of item statistics.
	 *
	 * @return the list of item statistics
	 */
	List<ItemStat> getItemStats();

	/**
	 * Retrieves the list of MobStat objects.
	 *
	 * @return the list of MobStat objects
	 */
	List<MobStat> getMobStats();

	/**
	 * Saves all the general stats asynchronously.
	 *
	 * @return a CompletableFuture representing the completion of the saving process
	 */
	CompletableFuture<Void> saveGeneralStats();

	/**
	 * Saves all the item stats asynchronously.
	 *
	 * @return a CompletableFuture representing the completion of the saving process
	 */
	CompletableFuture<Void> saveItemStats();

	/**
	 * Saves all the MobStats asynchronously.
	 *
	 * @return a CompletableFuture representing the completion of the saving process
	 */
	CompletableFuture<Void> saveMobStats();

	/**
	 * Saves all stats asynchronously.
	 *
	 * @return a CompletableFuture that completes when all stats are saved
	 */
	CompletableFuture<Void> saveStats();

	/**
	 * Retrieves a GeneralStat with the given name.
	 *
	 * @param name the name of the GeneralStat to retrieve
	 *
	 * @return an Optional containing the GeneralStat if found, otherwise an empty Optional
	 */
	@NotNull Optional<GeneralStat> getGeneralStat(String name);

	/**
	 * Retrieves an ItemStat with the given name.
	 *
	 * @param name the name of the ItemStat to retrieve
	 *
	 * @return an Optional containing the ItemStat if found, otherwise an empty Optional
	 */
	@NotNull Optional<ItemStat> getItemStat(String name);

	/**
	 * Retrieves a MobStat with the given name.
	 *
	 * @param name the name of the MobStat to retrieve
	 *
	 * @return an Optional containing the MobStat if found, otherwise an empty Optional
	 */
	@NotNull Optional<MobStat> getMobStat(String name);

	/**
	 * Adds a GeneralStat to the list of generalStats.
	 *
	 * @param generalStat the GeneralStat to add
	 *
	 * @return the added GeneralStat
	 */
	GeneralStat addGeneralStat(GeneralStat generalStat);

	/**
	 * Adds an ItemStat to the list of itemStats.
	 *
	 * @param itemStat the ItemStat to add
	 *
	 * @return the added ItemStat
	 */
	ItemStat addItemStat(ItemStat itemStat);

	/**
	 * Adds a MobStat to the list of mobStats.
	 *
	 * @param mobStat the MobStat to add
	 *
	 * @return the added MobStat
	 */
	MobStat addMobStat(MobStat mobStat);
}

