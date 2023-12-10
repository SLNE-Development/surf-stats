package dev.slne.stats.api.player;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.stat.GeneralStat;
import dev.slne.stats.api.stat.ItemStat;
import dev.slne.stats.api.stat.MobStat;
import dev.slne.stats.api.stat.repository.GeneralStatRepository;
import dev.slne.stats.api.stat.repository.ItemStatRepository;
import dev.slne.stats.api.stat.repository.MobStatRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * StatPlayer represents a player's statistics in a game. It provides methods to load and save the player's stats,
 * as well as retrieve specific stats and add new stats.
 */
@ApiStatus.NonExtendable
public final class StatPlayer {

	private final UUID uuid;

	private final List<GeneralStat> generalStats;
	private final List<ItemStat> itemStats;
	private final List<MobStat> mobStats;

	private final GeneralStatRepository generalStatRepository;
	private final ItemStatRepository itemStatRepository;
	private final MobStatRepository mobStatRepository;

	private boolean loaded;
	private boolean saving;
	private boolean disconnected;

	/**
	 * Constructs a new instance of StatPlayer with the specified UUID.
	 *
	 * @param uuid the UUID of the player
	 */
	@ApiStatus.Internal
	public StatPlayer(UUID uuid) {
		this.uuid = uuid;

		this.generalStats = new ArrayList<>();
		this.itemStats = new ArrayList<>();
		this.mobStats = new ArrayList<>();

		this.generalStatRepository =
			StatsApi.getInstance().getApplicationContext().getBean(GeneralStatRepository.class);
		this.itemStatRepository = StatsApi.getInstance().getApplicationContext().getBean(ItemStatRepository.class);
		this.mobStatRepository = StatsApi.getInstance().getApplicationContext().getBean(MobStatRepository.class);

		this.loaded = false;
		this.saving = false;
		this.disconnected = false;
	}

	/**
	 * Returns the current saving status of the StatPlayer instance.
	 *
	 * @return true if the StatPlayer instance is currently saving, false otherwise
	 */
	@Contract(pure = true)
	public boolean saving() {
		return saving;
	}

	/**
	 * Saves the current state of the "saving" flag for the StatPlayer.
	 *
	 * @param saving the value indicating whether the saving flag should be enabled or disabled
	 * @return the current instance of StatPlayer
	 */
	@Contract(value = "_ -> this", mutates = "this")
	public StatPlayer saving(boolean saving) {
		this.saving = saving;

		return this;
	}

	/**
	 * Determines if the player is disconnected.
	 *
	 * @return true if the player is disconnected, false otherwise.
	 */
	@Contract(pure = true)
	public boolean disconnected() {
		return disconnected;
	}

	/**
	 * Sets the disconnected status of the player.
	 *
	 * @param disconnected the disconnected status of the player
	 * @return the modified StatPlayer object
	 */
	@Contract(value = "_ -> this", mutates = "this")
	public StatPlayer disconnected(boolean disconnected) {
		this.disconnected = disconnected;
		return this;
	}

	/**
	 * Checks if the instance of StatPlayer is loaded.
	 *
	 * @return true if the StatPlayer instance is loaded; otherwise, false.
	 */
	@Contract(pure = true)
	public boolean loaded() {
		return loaded;
	}

	/**
	 * Sets the loaded status of the player.
	 *
	 * @param loaded true if the player is loaded, false otherwise
	 * @return the instance of the {@link StatPlayer}
	 */
	@Contract(value = "_ -> this", mutates = "this")
	public StatPlayer loaded(boolean loaded) {
		this.loaded = loaded;
		return this;
	}

	/**
	 * Loads the statistics for the player asynchronously.
	 *
	 * @return a CompletableFuture that completes when the statistics are loaded
	 */
	public CompletableFuture<Void> loadStats() {
		this.loaded = false;

		return CompletableFuture.allOf(loadGeneralStats(), loadItemStats(), loadMobStats()).thenRunAsync(() -> {
			this.loaded = true;
		}).exceptionally(throwable -> {
			StatsApi.getInstance().disconnectPlayer(this, Component.text("Failed to load stats", NamedTextColor.RED));

			throw new RuntimeException(throwable);
		});
	}

	/**
	 * Loads the general stats for the player.
	 * Clears the existing general stats and retrieves the general stats from the generalStatRepository.
	 *
	 * @return a CompletableFuture containing a list of GeneralStat objects
	 */
	private CompletableFuture<List<GeneralStat>> loadGeneralStats() {
		this.generalStats.clear();

		return generalStatRepository.findByStatOwnerAndServer(uuid, StatsApi.getServer()).thenApply(generalStats -> {
			this.generalStats.addAll(generalStats);

			return generalStats;
		});
	}

	/**
	 * Loads the item statistics for the player.
	 *
	 * @return a CompletableFuture that returns a List of ItemStat objects representing the loaded item statistics
	 */
	private CompletableFuture<List<ItemStat>> loadItemStats() {
		this.itemStats.clear();

		return itemStatRepository.findByStatOwnerAndServer(uuid, StatsApi.getServer()).thenApply(itemStats -> {
			this.itemStats.addAll(itemStats);

			return itemStats;
		});
	}

	/**
	 * Loads the MobStats for the player.
	 *
	 * @return a CompletableFuture that completes with a List of MobStat objects representing the loaded MobStats
	 */
	private CompletableFuture<List<MobStat>> loadMobStats() {
		this.mobStats.clear();

		return mobStatRepository.findByStatOwnerAndServer(uuid, StatsApi.getServer()).thenApply(mobStats -> {
			this.mobStats.addAll(mobStats);

			return mobStats;
		});
	}

	/**
	 * Retrieves the UUID associated with the player.
	 *
	 * @return the UUID of the player
	 */
	@Contract(pure = true)
	public UUID uuid() {
		return uuid;
	}

	/**
	 * Retrieves the list of general statistics.
	 *
	 * @return the list of general statistics
	 */
	@Contract(pure = true)
	public List<GeneralStat> generalStats() {
		return generalStats;
	}

	/**
	 * Returns the list of item statistics.
	 *
	 * @return the list of item statistics
	 */
	@Contract(pure = true)
	public List<ItemStat> itemStats() {
		return itemStats;
	}

	/**
	 * Returns the list of MobStat objects.
	 *
	 * @return the list of MobStat objects.
	 */
	@Contract(pure = true)
	public List<MobStat> mobStats() {
		return mobStats;
	}

	/**
	 * Saves all the general stats asynchronously.
	 *
	 * @return a CompletableFuture representing the completion of the saving process
	 */
	@Contract(" -> new")
	private @NotNull CompletableFuture<Void> saveGeneralStats() {
		return CompletableFuture.runAsync(() -> {
			for (GeneralStat stat : generalStats) {
				saveGeneralStat(stat).join();
			}
		});
	}

	/**
	 * Saves the item statistics asynchronously.
	 *
	 * @return a CompletableFuture representing the completion of the saving process
	 */
	@Contract(" -> new")
	private @NotNull CompletableFuture<Void> saveItemStats() {
		return CompletableFuture.runAsync(() -> {
			for (ItemStat stat : itemStats) {
				saveItemStat(stat).join();
			}
		});
	}

	/**
	 * Saves the MobStat objects asynchronously.
	 *
	 * @return A CompletableFuture representing the completion of the save operation.
	 */
	@Contract(" -> new")
	private @NotNull CompletableFuture<Void> saveMobStats() {
		return CompletableFuture.runAsync(() -> {
			for (MobStat stat : mobStats) {
				saveMobStat(stat).join();
			}
		});
	}

	/**
	 * Saves all stats asynchronously.
	 *
	 * @return a CompletableFuture that completes when all stats are saved
	 * @throws RuntimeException if an exception occurs during saving
	 */
	public CompletableFuture<Void> saveStats() {
		this.saving = true;

		return CompletableFuture.allOf(saveGeneralStats(), saveItemStats(), saveMobStats()).thenRunAsync(() -> {
			this.saving = false;
		}).exceptionally(throwable -> {
			this.saving = false;

			throw new RuntimeException(throwable);
		});
	}

	/**
	 * Retrieves a GeneralStat with the given name.
	 *
	 * @param name the name of the GeneralStat to retrieve
	 * @return an Optional containing the GeneralStat if found, otherwise an empty Optional
	 */
	public @NotNull Optional<GeneralStat> getGeneralStat(String name) {
		return generalStats.stream().filter(stat -> stat.generalKey().equals(name)).findFirst();
	}

	/**
	 * Retrieves the {@link ItemStat} with the specified name.
	 *
	 * @param name the name of the item stat to retrieve
	 * @return an {@link Optional} containing the item stat if found, or an empty {@link Optional} otherwise
	 */
	public @NotNull Optional<ItemStat> getItemStat(String name) {
		return itemStats.stream().filter(stat -> stat.itemKey().equals(name)).findFirst();
	}

	/**
	 * Retrieves the MobStat object with the specified name.
	 *
	 * @param name the name of the mob stat
	 * @return an Optional containing the MobStat object if found, otherwise an empty Optional
	 */
	public @NotNull Optional<MobStat> getMobStat(String name) {
		return mobStats.stream().filter(stat -> stat.mobKey().equals(name)).findFirst();
	}

	/**
	 * Saves a GeneralStat asynchronously.
	 *
	 * @param stat the GeneralStat object to be saved
	 * @return a CompletableFuture representing the save operation
	 */
	@Contract("_ -> new")
	private @NotNull CompletableFuture<GeneralStat> saveGeneralStat(GeneralStat stat) {
		return CompletableFuture.supplyAsync(() -> generalStatRepository.save(stat));
	}

	/**
	 * Saves an item stat asynchronously.
	 *
	 * @param stat the item stat to save
	 * @return a CompletableFuture that will be completed with the saved item stat
	 */
	@Contract("_ -> new")
	private @NotNull CompletableFuture<ItemStat> saveItemStat(ItemStat stat) {
		return CompletableFuture.supplyAsync(() -> itemStatRepository.save(stat));
	}

	/**
	 * Saves a MobStat asynchronously.
	 *
	 * @param stat the MobStat to be saved
	 * @return a CompletableFuture that completes with the saved MobStat
	 */
	@Contract("_ -> new")
	private @NotNull CompletableFuture<MobStat> saveMobStat(MobStat stat) {
		return CompletableFuture.supplyAsync(() -> mobStatRepository.save(stat));
	}

	/**
	 * Adds a GeneralStat to the list of general stats.
	 *
	 * @param generalStat the GeneralStat to add
	 * @return the added GeneralStat
	 */
	@Contract("_ -> param1")
	public GeneralStat addGeneralStat(GeneralStat generalStat) {
		generalStats.add(generalStat);

		return generalStat;
	}

	/**
	 * Adds an ItemStat to the list of itemStats.
	 *
	 * @param itemStat the ItemStat to add
	 * @return the added ItemStat
	 */
	@Contract("_ -> param1")
	public ItemStat addItemStat(ItemStat itemStat) {
		itemStats.add(itemStat);

		return itemStat;
	}

	/**
	 * Adds a MobStat to the StatPlayer.
	 *
	 * @param mobStat the MobStat to add
	 * @return the added MobStat
	 */
	@Contract("_ -> param1")
	public MobStat addMobStat(MobStat mobStat) {
		mobStats.add(mobStat);

		return mobStat;
	}

	@Contract(value = "null -> false", pure = true)
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof StatPlayer that)) return false;

		if (loaded != that.loaded) return false;
		return Objects.equals(uuid, that.uuid);
	}

	@Contract(pure = true)
	@Override
	public int hashCode() {
		int result = uuid != null ? uuid.hashCode() : 0;
		result = 31 * result + (loaded ? 1 : 0);
		return result;
	}
}
