package dev.slne.stats.bukkit;

import dev.slne.stats.api.stat.GeneralStat;
import dev.slne.stats.api.stat.ItemStat;
import dev.slne.stats.api.stat.MobStat;
import feign.Request;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * The interface Temp feign client.
 */
@FeignClient(name = "temp", url = "https://dapi.slne.dev/api/stats/temp",
	configuration = TempFeignClient.CustomFeignClientConfig.class)
public interface TempFeignClient {

	/**
	 * Batch save general.
	 *
	 * @param generalStats the general stats
	 */
	@PostMapping("/general")
	void batchSaveGeneral(GeneralStat[] generalStats);

	/**
	 * Batch save item.
	 *
	 * @param itemStats the item stats
	 */
	@PostMapping("/item")
	void batchSaveItem(ItemStat[] itemStats);

	/**
	 * Batch save mob.
	 *
	 * @param mobStats the mob stats
	 */
	@PostMapping("/mob")
	void batchSaveMob(MobStat[] mobStats);

	class CustomFeignClientConfig {

		@Bean
		public Request.Options requestOptions() {
			return new Request.Options(Integer.MAX_VALUE, Integer.MAX_VALUE);
		}

	}
}
