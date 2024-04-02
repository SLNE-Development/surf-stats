package dev.slne.stats.core.feign;

import dev.slne.stats.api.stat.GeneralStat;
import dev.slne.stats.api.stat.ItemStat;
import dev.slne.stats.api.stat.MobStat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The interface Stat client.
 */
@FeignClient(name = "stat-service", url = "https://dapi.slne.dev/api/stats")
public interface StatClient {

	/**
	 * Gets general stats by uuid and server sync.
	 *
	 * @param uuid   the uuid
	 * @param server the server
	 *
	 * @return the general stats by uuid and server sync
	 */
	@GetMapping(value = "/{uuid}/general/{server}")
	List<GeneralStat> getGeneralStatsByUuidAndServerSync(
		@PathVariable("uuid") UUID uuid,
		@PathVariable("server") String server
	);

	/**
	 * Gets item stats by uuid and server sync.
	 *
	 * @param uuid   the uuid
	 * @param server the server
	 *
	 * @return the item stats by uuid and server sync
	 */
	@GetMapping(value = "/{uuid}/item/{server}")
	List<ItemStat> getItemStatsByUuidAndServerSync(
		@PathVariable("uuid") UUID uuid,
		@PathVariable("server") String server
	);

	/**
	 * Gets mob stats by uuid and server sync.
	 *
	 * @param uuid   the uuid
	 * @param server the server
	 *
	 * @return the mob stats by uuid and server sync
	 */
	@GetMapping(value = "/{uuid}/mob/{server}")
	List<MobStat> getMobStatsByUuidAndServerSync(
		@PathVariable("uuid") UUID uuid,
		@PathVariable("server") String server
	);

	/**
	 * Save general stats.
	 *
	 * @param uuid         the uuid
	 * @param generalStats the general stats
	 */
	@PostMapping(value = "/{uuid}/general")
	void saveGeneralStatsSync(@PathVariable("uuid") UUID uuid, List<GeneralStat> generalStats);

	/**
	 * Save item stats.
	 *
	 * @param uuid      the uuid
	 * @param itemStats the item stats
	 */
	@PostMapping(value = "/{uuid}/item")
	void saveItemStatsSync(@PathVariable("uuid") UUID uuid, List<ItemStat> itemStats);

	/**
	 * Save mob stats.
	 *
	 * @param uuid     the uuid
	 * @param mobStats the mob stats
	 */
	@PostMapping(value = "/{uuid}/mob")
	void saveMobStatsSync(@PathVariable("uuid") UUID uuid, List<MobStat> mobStats);

	/**
	 * Gets general stats by uuid and server.
	 *
	 * @param uuid   the uuid
	 * @param server the server
	 *
	 * @return the general stats by uuid and server
	 */
	default CompletableFuture<List<GeneralStat>> getGeneralStatsByUuidAndServer(UUID uuid, String server) {
		return CompletableFuture.supplyAsync(() -> getGeneralStatsByUuidAndServerSync(uuid, server));
	}

	/**
	 * Gets item stats by uuid and server.
	 *
	 * @param uuid   the uuid
	 * @param server the server
	 *
	 * @return the item stats by uuid and server
	 */
	default CompletableFuture<List<ItemStat>> getItemStatsByUuidAndServer(UUID uuid, String server) {
		return CompletableFuture.supplyAsync(() -> getItemStatsByUuidAndServerSync(uuid, server));
	}

	/**
	 * Gets mob stats by uuid and server.
	 *
	 * @param uuid   the uuid
	 * @param server the server
	 *
	 * @return the mob stats by uuid and server
	 */
	default CompletableFuture<List<MobStat>> getMobStatsByUuidAndServer(UUID uuid, String server) {
		return CompletableFuture.supplyAsync(() -> getMobStatsByUuidAndServerSync(uuid, server));
	}

	/**
	 * Save general stats.
	 *
	 * @param uuid         the uuid
	 * @param generalStats the general stats
	 *
	 * @return the completable future
	 */
	default CompletableFuture<Void> saveGeneralStats(UUID uuid, List<GeneralStat> generalStats) {
		return CompletableFuture.runAsync(() -> saveGeneralStatsSync(uuid, generalStats));
	}

	/**
	 * Save item stats.
	 *
	 * @param uuid      the uuid
	 * @param itemStats the item stats
	 *
	 * @return the completable future
	 */
	default CompletableFuture<Void> saveItemStats(UUID uuid, List<ItemStat> itemStats) {
		return CompletableFuture.runAsync(() -> saveItemStatsSync(uuid, itemStats));
	}

	/**
	 * Save mob stats.
	 *
	 * @param uuid     the uuid
	 * @param mobStats the mob stats
	 *
	 * @return the completable future
	 */
	default CompletableFuture<Void> saveMobStats(UUID uuid, List<MobStat> mobStats) {
		return CompletableFuture.runAsync(() -> saveMobStatsSync(uuid, mobStats));
	}

}
