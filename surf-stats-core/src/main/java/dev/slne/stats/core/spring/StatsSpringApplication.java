package dev.slne.stats.core.spring;

import dev.slne.data.api.DataApi;
import dev.slne.data.api.spring.SurfSpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The StatsSpringApplication class is the entry point for running the statistics Spring application context.
 * It provides a method to run the configurable application context.
 */
@SurfSpringApplication(
	scanBasePackages = "dev.slne.stats",
	scanFeignBasePackages = "dev.slne.stats"
)
public class StatsSpringApplication {

	/**
	 * The StatsSpringApplication class is the entry point for running the statistics Spring application context.
	 * It provides a method to run the configurable application context.
	 *
	 * @param classLoader the class loader to use
	 *
	 * @return configurable application context
	 */
	public static ConfigurableApplicationContext run(ClassLoader classLoader) {
		return DataApi.run(StatsSpringApplication.class, classLoader);
	}
}
