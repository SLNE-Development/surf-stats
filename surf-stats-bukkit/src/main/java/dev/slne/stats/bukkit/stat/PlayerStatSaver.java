package dev.slne.stats.bukkit.stat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The type Player stat saver.
 */
public class PlayerStatSaver {

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
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Gets player stats json.
	 *
	 * @param player the player
	 *
	 * @return the player stats json
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
	 * Save stats.
	 *
	 * @param player the player
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
	 * Cb class string.
	 *
	 * @param clazz the clazz
	 *
	 * @return the string
	 */
	private static String cbClass(String clazz) {
		return CB_PACKAGE + "." + clazz;
	}

}
