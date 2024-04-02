package dev.slne.stats.bukkit.stat.reflections.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

/**
 * The interface Server stats counter proxy.
 */
@Proxies(className = "net.minecraft.stats.ServerStatsCounter")
public interface ServerStatsCounterProxy {

	/**
	 * Save.
	 *
	 * @param serverStatsCounter the server stats counter
	 */
	@MethodName("save")
	void save(Object serverStatsCounter);

	/**
	 * To json string.
	 *
	 * @param serverStatsCounter the server stats counter
	 *
	 * @return the string
	 */
	@MethodName("toJson")
	String toJson(Object serverStatsCounter);
}
