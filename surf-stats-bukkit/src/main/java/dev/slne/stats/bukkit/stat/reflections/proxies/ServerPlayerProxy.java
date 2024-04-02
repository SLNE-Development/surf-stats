package dev.slne.stats.bukkit.stat.reflections.proxies;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

/**
 * The interface Server player proxy.
 */
@Proxies(className = "net.minecraft.server.level.ServerPlayer")
public interface ServerPlayerProxy {

	/**
	 * Gets stats.
	 *
	 * @param serverPlayer the server player
	 *
	 * @return the stats
	 */
	@MethodName("getStats")
	Object getStats(Object serverPlayer);
}
