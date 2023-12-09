package dev.slne.stats.api.instance;

import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.player.StatPlayerManager;
import dev.slne.stats.api.stat.file.PlayerStatFile;
import dev.slne.stats.api.stat.processor.processors.StatProcessors;
import net.kyori.adventure.text.Component;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.UUID;

/**
 * The interface Stats instance.
 */
public interface StatsInstance {

	/**
	 * Gets application context.
	 *
	 * @return the application context
	 */
	ConfigurableApplicationContext getApplicationContext();

	/**
	 * On load.
	 */
	void onLoad();

	/**
	 * On enable.
	 */
	void onEnable();

	/**
	 * On disable.
	 */
	void onDisable();

	/**
	 * Gets stat player.
	 *
	 * @param uuid the uuid
	 *
	 * @return the stat player
	 */
	StatPlayer getStatPlayer(UUID uuid);

	/**
	 * Gets stat player manager.
	 *
	 * @return the stat player manager
	 */
	StatPlayerManager getStatPlayerManager();

	/**
	 * Gets player stat file reader.
	 *
	 * @return the player stat file reader
	 */
	PlayerStatFile.Reader getPlayerStatFileReader();

	/**
	 * Gets stat folder.
	 *
	 * @return the stat folder
	 */
	File getStatFolder();

	/**
	 * Sets stat folder.
	 *
	 * @param statFolder the stat folder
	 */
	void setStatFolder(File statFolder);

	/**
	 * Gets stat processor.
	 *
	 * @return the stat processor
	 */
	StatProcessors getStatProcessor();
	
	/**
	 * Disconnect player.
	 *
	 * @param statPlayer the stat player
	 * @param reason     the reason
	 */
	void disconnectPlayer(StatPlayer statPlayer, Component reason);

}
