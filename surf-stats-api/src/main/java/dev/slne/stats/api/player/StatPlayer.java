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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The type Stat player.
 */
public class StatPlayer {

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
	 * Instantiates a new Stat player.
	 *
	 * @param uuid the uuid
	 */
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
	 * Saving boolean.
	 *
	 * @return the boolean
	 */
	public boolean saving() {
		return saving;
	}

	/**
	 * Saving stat player.
	 *
	 * @param saving the saving
	 *
	 * @return the stat player
	 */
	public StatPlayer saving(boolean saving) {
		this.saving = saving;
		return this;
	}

	/**
	 * Disconnected boolean.
	 *
	 * @return the boolean
	 */
	public boolean disconnected() {
		return disconnected;
	}

	/**
	 * Disconnected stat player.
	 *
	 * @param disconnected the disconnected
	 *
	 * @return the stat player
	 */
	public StatPlayer disconnected(boolean disconnected) {
		this.disconnected = disconnected;
		return this;
	}

	/**
	 * Loaded boolean.
	 *
	 * @return the boolean
	 */
	public boolean loaded() {
		return loaded;
	}

	/**
	 * Loaded stat player.
	 *
	 * @param loaded the loaded
	 *
	 * @return the stat player
	 */
	public StatPlayer loaded(boolean loaded) {
		this.loaded = loaded;
		return this;
	}

	/**
	 * Load stats completable future.
	 *
	 * @return the completable future
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
	 * Load general stats completable future.
	 *
	 * @return the completable future
	 */
	private CompletableFuture<List<GeneralStat>> loadGeneralStats() {
		this.generalStats.clear();

		return generalStatRepository.findByStatOwnerAndServer(uuid, StatsApi.getServer()).thenApply(generalStats -> {
			this.generalStats.addAll(generalStats);

			return generalStats;
		});
	}

	/**
	 * Load item stats completable future.
	 *
	 * @return the completable future
	 */
	private CompletableFuture<List<ItemStat>> loadItemStats() {
		this.itemStats.clear();

		return itemStatRepository.findByStatOwnerAndServer(uuid, StatsApi.getServer()).thenApply(itemStats -> {
			this.itemStats.addAll(itemStats);

			return itemStats;
		});
	}

	/**
	 * Load mob stats completable future.
	 *
	 * @return the completable future
	 */
	private CompletableFuture<List<MobStat>> loadMobStats() {
		this.mobStats.clear();

		return mobStatRepository.findByStatOwnerAndServer(uuid, StatsApi.getServer()).thenApply(mobStats -> {
			this.mobStats.addAll(mobStats);

			return mobStats;
		});
	}

	/**
	 * Uuid uuid.
	 *
	 * @return the uuid
	 */
	public UUID uuid() {
		return uuid;
	}

	/**
	 * General stats list.
	 *
	 * @return the list
	 */
	public List<GeneralStat> generalStats() {
		return generalStats;
	}

	/**
	 * Item stats list.
	 *
	 * @return the list
	 */
	public List<ItemStat> itemStats() {
		return itemStats;
	}

	/**
	 * Mob stats list.
	 *
	 * @return the list
	 */
	public List<MobStat> mobStats() {
		return mobStats;
	}

	/**
	 * Save general stats completable future.
	 *
	 * @return the completable future
	 */
	private CompletableFuture<Void> saveGeneralStats() {
		return CompletableFuture.runAsync(() -> {
			for (GeneralStat stat : generalStats) {
				saveGeneralStat(stat).join();
			}
		});
	}

	/**
	 * Save item stats completable future.
	 *
	 * @return the completable future
	 */
	private CompletableFuture<Void> saveItemStats() {
		return CompletableFuture.runAsync(() -> {
			for (ItemStat stat : itemStats) {
				saveItemStat(stat).join();
			}
		});
	}

	/**
	 * Save mob stats completable future.
	 *
	 * @return the completable future
	 */
	private CompletableFuture<Void> saveMobStats() {
		return CompletableFuture.runAsync(() -> {
			for (MobStat stat : mobStats) {
				saveMobStat(stat).join();
			}
		});
	}

	/**
	 * Save stats completable future.
	 *
	 * @return the completable future
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
	 * Gets general stat.
	 *
	 * @param name the name
	 *
	 * @return the general stat
	 */
	public Optional<GeneralStat> getGeneralStat(String name) {
		return generalStats.stream().filter(stat -> stat.generalKey().equals(name)).findFirst();
	}

	/**
	 * Gets item stat.
	 *
	 * @param name the name
	 *
	 * @return the item stat
	 */
	public Optional<ItemStat> getItemStat(String name) {
		return itemStats.stream().filter(stat -> stat.itemKey().equals(name)).findFirst();
	}

	/**
	 * Gets mob stat.
	 *
	 * @param name the name
	 *
	 * @return the mob stat
	 */
	public Optional<MobStat> getMobStat(String name) {
		return mobStats.stream().filter(stat -> stat.mobKey().equals(name)).findFirst();
	}

	/**
	 * Save general stat general stat.
	 *
	 * @param stat the stat
	 *
	 * @return the general stat
	 */
	private CompletableFuture<GeneralStat> saveGeneralStat(GeneralStat stat) {
		return CompletableFuture.supplyAsync(() -> generalStatRepository.save(stat));
	}

	/**
	 * Save item stat item stat.
	 *
	 * @param stat the stat
	 *
	 * @return the item stat
	 */
	private CompletableFuture<ItemStat> saveItemStat(ItemStat stat) {
		return CompletableFuture.supplyAsync(() -> itemStatRepository.save(stat));
	}

	/**
	 * Save mob stat mob stat.
	 *
	 * @param stat the stat
	 *
	 * @return the mob stat
	 */
	private CompletableFuture<MobStat> saveMobStat(MobStat stat) {
		return CompletableFuture.supplyAsync(() -> mobStatRepository.save(stat));
	}

	/**
	 * Add general stat general stat.
	 *
	 * @param generalStat the general stat
	 *
	 * @return the general stat
	 */
	public GeneralStat addGeneralStat(GeneralStat generalStat) {
		generalStats.add(generalStat);

		return generalStat;
	}

	/**
	 * Add item stat item stat.
	 *
	 * @param itemStat the item stat
	 *
	 * @return the item stat
	 */
	public ItemStat addItemStat(ItemStat itemStat) {
		itemStats.add(itemStat);

		return itemStat;
	}

	/**
	 * Add mob stat mob stat.
	 *
	 * @param mobStat the mob stat
	 *
	 * @return the mob stat
	 */
	public MobStat addMobStat(MobStat mobStat) {
		mobStats.add(mobStat);

		return mobStat;
	}
}
