package dev.slne.stats.bukkit.stat;

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
	private static final Class<?> SERVER_PLAYER_CLASS;
	private static final Class<?> SERVER_STATS_COUNTER_CLASS;

	private static final Method GET_HANDLE_METHOD;
	private static final Method GET_STATS_METHOD;
	private static final Method SAVE_METHOD;
	private static final Method TO_JSON_METHOD;

	static {
		try {
			CRAFT_PLAYER_CLASS = Class.forName(cbClass("entity.CraftPlayer"));
			SERVER_PLAYER_CLASS = Class.forName("net.minecraft.server.level.EntityPlayer");
			SERVER_STATS_COUNTER_CLASS = Class.forName("net.minecraft.stats.ServerStatisticManager");

			GET_HANDLE_METHOD = CRAFT_PLAYER_CLASS.getDeclaredMethod("getHandle");
			GET_STATS_METHOD = SERVER_PLAYER_CLASS.getDeclaredMethod("F");
			SAVE_METHOD = SERVER_STATS_COUNTER_CLASS.getDeclaredMethod("a");
			TO_JSON_METHOD = SERVER_STATS_COUNTER_CLASS.getDeclaredMethod("b");
			TO_JSON_METHOD.setAccessible(true);
		} catch (ClassNotFoundException | NoSuchMethodException exception) {
			ComponentLogger.logger().error("Failed to initialize PlayerStatSaver", exception);
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Returns the JSON representation of a player's statistics.
	 *
	 * @param player The player for which to retrieve the statistics.
	 * @return The JSON representation of the player's statistics.
	 * @throws RuntimeException if an error occurs while retrieving the statistics.
	 */
	public static String getPlayerStatsJson(Player player) {
		try {
			Object craftPlayer = CRAFT_PLAYER_CLASS.cast(player);
			Object serverPlayer = SERVER_PLAYER_CLASS.cast(GET_HANDLE_METHOD.invoke(craftPlayer));
			Object serverStatsCounter = SERVER_STATS_COUNTER_CLASS.cast(GET_STATS_METHOD.invoke(serverPlayer));

			return (String) TO_JSON_METHOD.invoke(serverStatsCounter);
		} catch (IllegalAccessException | InvocationTargetException exception) {
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Saves the statistics of a player to a file.
	 *
	 * @param player The player whose statistics need to be saved.
	 * @throws RuntimeException If an error occurs while saving the statistics.
	 */
	public static void saveStatsToFile(Player player) {
		try {
			Object craftPlayer = CRAFT_PLAYER_CLASS.cast(player);
			Object serverPlayer = SERVER_PLAYER_CLASS.cast(GET_HANDLE_METHOD.invoke(craftPlayer));
			Object serverStatsCounter = SERVER_STATS_COUNTER_CLASS.cast(GET_STATS_METHOD.invoke(serverPlayer));

			SAVE_METHOD.invoke(serverStatsCounter);
		} catch (IllegalAccessException | InvocationTargetException exception) {
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Concatenates the given class name with the CB_PACKAGE constant to form a fully qualified class name.
	 *
	 * @param clazz the class name to concatenate with CB_PACKAGE
	 * @return the fully qualified class name formed by concatenating CB_PACKAGE and clazz
	 */
	@Contract(pure = true)
	private static @NotNull String cbClass(String clazz) {
		return CB_PACKAGE + "." + clazz;
	}

}
