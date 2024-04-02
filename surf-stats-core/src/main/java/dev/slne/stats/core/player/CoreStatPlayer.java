package dev.slne.stats.core.player;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.GeneralStat;
import dev.slne.stats.api.stat.ItemStat;
import dev.slne.stats.api.stat.MobStat;
import dev.slne.stats.core.feign.StatClient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.util.Lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The type Core stat player.
 */
public class CoreStatPlayer implements StatPlayer {

	private final UUID uuid;

	private final List<GeneralStat> generalStats;
	private final List<ItemStat> itemStats;
	private final List<MobStat> mobStats;

	private final Lazy<StatClient> statClient =
		Lazy.of(() -> StatsApi.getInstance().getApplicationContext().getBean(StatClient.class));

	private boolean loaded;
	private boolean saving;
	private boolean disconnected;

	/**
	 * Constructs a new instance of StatPlayer with the specified UUID.
	 *
	 * @param uuid the UUID of the player
	 */
	@ApiStatus.Internal
	public CoreStatPlayer(UUID uuid) {
		this.uuid = uuid;

		this.generalStats = new ArrayList<>();
		this.itemStats = new ArrayList<>();
		this.mobStats = new ArrayList<>();

		this.loaded = false;
		this.saving = false;
		this.disconnected = false;
	}

	@Override
	public boolean isSaving() {
		return saving;
	}

	@Override
	public StatPlayer setSaving(boolean saving) {
		this.saving = saving;

		return this;
	}

	@Override
	public boolean isDisconnected() {
		return disconnected;
	}

	@Override
	public StatPlayer setDisconnected(boolean disconnected) {
		this.disconnected = disconnected;
		return this;
	}

	@Override
	public boolean isLoaded() {
		return loaded;
	}

	@Override
	public StatPlayer setLoaded(boolean loaded) {
		this.loaded = loaded;
		return this;
	}

	@Override
	public CompletableFuture<Void> loadStats() {
		this.loaded = false;

		return CompletableFuture.allOf(loadGeneralStats(), loadItemStats(), loadMobStats()).thenRunAsync(() -> {
			this.loaded = true;
		}).exceptionally(throwable -> {
			StatsApi.getInstance().disconnectPlayer(this, Component.text("Failed to load stats", NamedTextColor.RED));

			throw new RuntimeException(throwable);
		});
	}

	@Override
	public CompletableFuture<List<GeneralStat>> loadGeneralStats() {
		this.generalStats.clear();

		return statClient.get().getGeneralStatsByUuidAndServer(uuid, StatsApi.getServer()).thenApply(generalStats -> {
			this.generalStats.addAll(generalStats);

			return generalStats;
		});
	}

	@Override
	public CompletableFuture<List<ItemStat>> loadItemStats() {
		this.itemStats.clear();

		return statClient.get().getItemStatsByUuidAndServer(uuid, StatsApi.getServer()).thenApply(itemStats -> {
			this.itemStats.addAll(itemStats);

			return itemStats;
		});
	}

	@Override
	public CompletableFuture<List<MobStat>> loadMobStats() {
		this.mobStats.clear();

		return statClient.get().getMobStatsByUuidAndServer(uuid, StatsApi.getServer()).thenApply(mobStats -> {
			this.mobStats.addAll(mobStats);

			return mobStats;
		});
	}

	@Override
	public UUID getUuid() {
		return uuid;
	}

	@Override
	public List<GeneralStat> getGeneralStats() {
		return generalStats;
	}

	@Override
	public List<ItemStat> getItemStats() {
		return itemStats;
	}

	@Override
	public List<MobStat> getMobStats() {
		return mobStats;
	}

	@Override
	public @NotNull CompletableFuture<Void> saveGeneralStats() {
		return statClient.get().saveGeneralStats(uuid, generalStats.toArray(GeneralStat[]::new));
	}

	@Override
	public @NotNull CompletableFuture<Void> saveItemStats() {
		return statClient.get().saveItemStats(uuid, itemStats.toArray(ItemStat[]::new));
	}

	@Override
	public @NotNull CompletableFuture<Void> saveMobStats() {
		return statClient.get().saveMobStats(uuid, mobStats.toArray(MobStat[]::new));
	}

	@Override
	public CompletableFuture<Void> saveStats() {
		this.saving = true;

		return CompletableFuture.allOf(saveGeneralStats(), saveItemStats(), saveMobStats()).thenRunAsync(() -> {
			this.saving = false;
		}).exceptionally(throwable -> {
			this.saving = false;

			throw new RuntimeException(throwable);
		});
	}

	@Override
	public @NotNull Optional<GeneralStat> getGeneralStat(String name) {
		return generalStats.stream().filter(stat -> stat.getGeneralKey().equals(name)).findFirst();
	}

	@Override
	public @NotNull Optional<ItemStat> getItemStat(String name) {
		return itemStats.stream().filter(stat -> stat.getItemKey().equals(name)).findFirst();
	}

	@Override
	public @NotNull Optional<MobStat> getMobStat(String name) {
		return mobStats.stream().filter(stat -> stat.getMobKey().equals(name)).findFirst();
	}

	@Override
	public GeneralStat addGeneralStat(GeneralStat generalStat) {
		generalStats.add(generalStat);

		return generalStat;
	}

	@Override
	public ItemStat addItemStat(ItemStat itemStat) {
		itemStats.add(itemStat);

		return itemStat;
	}

	@Override
	public MobStat addMobStat(MobStat mobStat) {
		mobStats.add(mobStat);

		return mobStat;
	}

	@Override
	public int hashCode() {
		int result = uuid != null ? uuid.hashCode() : 0;
		result = 31 * result + ( loaded ? 1 : 0 );
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (!( other instanceof StatPlayer that )) {
			return false;
		}

		if (loaded != that.isLoaded()) {
			return false;
		}

		return Objects.equals(uuid, that.getUuid());
	}
}
