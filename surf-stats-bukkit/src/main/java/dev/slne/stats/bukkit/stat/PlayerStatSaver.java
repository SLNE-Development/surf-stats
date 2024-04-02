package dev.slne.stats.bukkit.stat;

import dev.slne.stats.bukkit.stat.reflections.Reflection;
import dev.slne.stats.bukkit.stat.reflections.proxies.ServerPlayerProxy;
import dev.slne.stats.bukkit.stat.reflections.proxies.ServerStatsCounterProxy;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The PlayerStatSaver class provides methods to save and retrieve player statistics.
 */
@ApiStatus.Internal
public final class PlayerStatSaver {

	private static final String CB_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();
	private static final Class<?> CRAFT_PLAYER_CLASS;
	private static final Method GET_HANDLE_METHOD;

	static {
		try {
			CRAFT_PLAYER_CLASS = Class.forName(cbClass("entity.CraftPlayer"));
			GET_HANDLE_METHOD = CRAFT_PLAYER_CLASS.getDeclaredMethod("getHandle");
		} catch (ClassNotFoundException | NoSuchMethodException exception) {
			ComponentLogger.logger().error("Failed to initialize PlayerStatSaver", exception);
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Returns the JSON representation of a player's statistics.
	 *
	 * @param player The player for which to retrieve the statistics.
	 *
	 * @return The JSON representation of the player's statistics.
	 *
	 * @throws RuntimeException if an error occurs while retrieving the statistics.
	 */
	public static String getPlayerStatsJson(Player player) {
		try {
			ServerPlayerProxy serverPlayerProxy = Reflection.SERVER_PLAYER_PROXY;
			ServerStatsCounterProxy serverStatsCounterProxy = Reflection.SERVER_STATS_COUNTER_PROXY;

			Object craftPlayer = CRAFT_PLAYER_CLASS.cast(player);
			Object serverPlayer = GET_HANDLE_METHOD.invoke(craftPlayer);
			Object serverStatsCounter = serverPlayerProxy.getStats(serverPlayer);

			return serverStatsCounterProxy.toJson(serverStatsCounter);
		} catch (IllegalAccessException | InvocationTargetException exception) {
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Saves the statistics of a player to a file.
	 *
	 * @param player The player whose statistics need to be saved.
	 *
	 * @throws RuntimeException If an error occurs while saving the statistics.
	 */
	public static void saveStatsToFile(Player player) {
		try {
			ServerPlayerProxy serverPlayerProxy = Reflection.SERVER_PLAYER_PROXY;
			ServerStatsCounterProxy serverStatsCounterProxy = Reflection.SERVER_STATS_COUNTER_PROXY;

			Object craftPlayer = CRAFT_PLAYER_CLASS.cast(player);
			Object serverPlayer = GET_HANDLE_METHOD.invoke(craftPlayer);
			Object serverStatsCounter = serverPlayerProxy.getStats(serverPlayer);

			serverStatsCounterProxy.save(serverStatsCounter);
		} catch (IllegalAccessException | InvocationTargetException exception) {
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Concatenates the given class name with the CB_PACKAGE constant to form a fully qualified class name.
	 *
	 * @param clazz the class name to concatenate with CB_PACKAGE
	 *
	 * @return the fully qualified class name formed by concatenating CB_PACKAGE and clazz
	 */
	@Contract(pure = true)
	private static @NotNull String cbClass(String clazz) {
		return CB_PACKAGE + "." + clazz;
	}

}
