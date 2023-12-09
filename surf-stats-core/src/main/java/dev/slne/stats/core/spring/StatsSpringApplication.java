package dev.slne.stats.core.spring;

import dev.slne.data.api.DataApi;
import dev.slne.data.api.spring.SurfSpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The type Stats spring application.
 */
@SurfSpringApplication(
	scanBasePackages = "dev.slne.stats",
	baseJpaPackages = "dev.slne.stats.api.stat.repository",
	baseRedisPackages = "dev.slne.stats.api.stat.redis",
	entityScanPackages = "dev.slne.stats"
)
public class StatsSpringApplication {

	/**
	 * Run configurable application context.
	 *
	 * @param classLoader the class loader
	 *
	 * @return the configurable application context
	 */
	public static ConfigurableApplicationContext run(ClassLoader classLoader) {
		return DataApi.run(StatsSpringApplication.class, classLoader);
	}
}
