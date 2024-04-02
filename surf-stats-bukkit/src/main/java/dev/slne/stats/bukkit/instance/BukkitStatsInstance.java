package dev.slne.stats.bukkit.instance;

import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.bukkit.StatsBukkitPlugin;
import dev.slne.stats.bukkit.listener.ListenerManager;
import dev.slne.stats.core.instance.CoreStatsInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * The class BukkitStatsInstance is a subclass of CoreStatsInstance and represents
 * a statistics instance for a Bukkit server implementation.
 */
@ApiStatus.Internal
public final class BukkitStatsInstance extends CoreStatsInstance {

	private ListenerManager listenerManager;

	/**
	 * Constructs a new instance of the BukkitStatsInstance class with the provided class loader.
	 *
	 * @param classLoader the class loader to be used with the BukkitStatsInstance
	 */
	public BukkitStatsInstance(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public void onLoad() {
		super.onLoad();

		this.listenerManager = new ListenerManager();
	}

	@Override
	public void onEnable() {
		super.onEnable();

		setStatFolder(new File(Bukkit.getServer().getWorlds().get(0).getWorldFolder(), "stats"));
		listenerManager.onEnable();
	}

	@Override
	public void onDisable() {
		listenerManager.onDisable();

		super.onDisable();
	}

	@Override
	public void disconnectPlayer(@NotNull StatPlayer statPlayer, Component reason) {
		Player player = Bukkit.getPlayer(statPlayer.getUuid());

		new BukkitRunnable() {
			@Override
			public void run() {
				if (player != null) {
					player.kick(reason);
				}
			}
		}.runTask(StatsBukkitPlugin.getInstance());
	}
}
