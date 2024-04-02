package dev.slne.stats.bukkit.stat.reflections;

import dev.slne.stats.bukkit.stat.reflections.proxies.ServerPlayerProxy;
import dev.slne.stats.bukkit.stat.reflections.proxies.ServerStatsCounterProxy;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;

/**
 * The type Reflection.
 */
public class Reflection {

	/**
	 * The constant SERVER_PLAYER_PROXY.
	 */
	public static final ServerPlayerProxy SERVER_PLAYER_PROXY;
	
	/**
	 * The constant SERVER_STATS_COUNTER_PROXY.
	 */
	public static final ServerStatsCounterProxy SERVER_STATS_COUNTER_PROXY;

	static {
		final ReflectionRemapper remapper = ReflectionRemapper.forReobfMappingsInPaperJar();
		final ReflectionProxyFactory proxyFactory =
			ReflectionProxyFactory.create(remapper, Reflection.class.getClassLoader()
			);

		SERVER_PLAYER_PROXY = proxyFactory.reflectionProxy(ServerPlayerProxy.class);
		SERVER_STATS_COUNTER_PROXY = proxyFactory.reflectionProxy(ServerStatsCounterProxy.class);

		System.gc();
	}

}
