package dev.slne.stats.bukkit.instance;

import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.bukkit.StatsBukkitPlugin;
import dev.slne.stats.bukkit.listener.ListenerManager;
import dev.slne.stats.core.instance.CoreStatsInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

/**
 * The type Bukkit stats instance.
 */
public class BukkitStatsInstance extends CoreStatsInstance {

	private ListenerManager listenerManager;

	/**
	 * Instantiates a new Bukkit stats instance.
	 *
	 * @param classLoader the class loader
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
	public void disconnectPlayer(StatPlayer statPlayer, Component reason) {
		Player player = Bukkit.getPlayer(statPlayer.uuid());

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
