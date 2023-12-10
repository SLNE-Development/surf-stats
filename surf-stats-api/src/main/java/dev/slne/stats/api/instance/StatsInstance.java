package dev.slne.stats.api.instance;

import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.player.StatPlayerManager;
import dev.slne.stats.api.stat.file.PlayerStatFile;
import dev.slne.stats.api.stat.processor.processors.StatProcessors;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.UUID;

/**
 * The StatsInstance interface represents a statistics instance in the application.
 * A statistics instance manages various statistics related operations and provides access to different components.
 */
@ApiStatus.NonExtendable
public interface StatsInstance {

	/**
	 * Retrieves the application context of the StatsInstance.
	 *
	 * @return the ConfigurableApplicationContext of the StatsInstance
	 */
	ConfigurableApplicationContext getApplicationContext();

	/**
	 * Executes the onLoad process for the StatsInstance.
	 * This method is called when the StatsInstance is being loaded.
	 * It initializes necessary components and sets up the environment for the instance.
	 * Subclasses of StatsInstance can override this method to perform custom onLoad operations.
	 */
	@ApiStatus.Internal
	void onLoad();

	/**
	 * Executes the onEnable process for the implementing class.
	 * This method is called when the implementing class is being enabled.
	 * It may initialize necessary components and setup the environment for the instance.
	 * Subclasses of the implementing class can override this method to perform custom onEnable operations.
	 *
	 * @see StatsInstance#onLoad()
	 * @see StatsInstance#onDisable()
	 */
	@ApiStatus.Internal
	void onEnable();

	/**
	 * Executes the onDisable process for the implementing class.
	 * This method is called when the implementing class is being disabled.
	 * It may clean up resources and perform any necessary cleanup operations.
	 * Subclasses of the implementing class can override this method to perform custom onDisable operations.
	 *
	 * @see StatsInstance#onEnable()
	 */
	@ApiStatus.Internal
	void onDisable();

	/**
	 * Retrieves the StatPlayer associated with the given UUID.
	 *
	 * @param uuid the UUID of the StatPlayer to retrieve
	 * @return the StatPlayer associated with the given UUID, or null if not found
	 */
	StatPlayer getStatPlayer(UUID uuid);

	/**
	 * Retrieves the StatPlayerManager instance.
	 *
	 * @return the StatPlayerManager instance
	 */
	StatPlayerManager getStatPlayerManager();

	/**
	 * Retrieves the reader for the player stat file.
	 *
	 * @return the reader for the player stat file
	 */
	PlayerStatFile.Reader getPlayerStatFileReader();

	/**
	 * Retrieves the folder where player statistics are stored.
	 *
	 * @return the folder where player statistics are stored
	 */
	File getStatFolder();

	/**
	 * Sets the folder where player statistics are stored.
	 *
	 * @param statFolder the folder where player statistics are stored
	 */
	@ApiStatus.Internal
	void setStatFolder(File statFolder);

	/**
	 * Retrieves the StatProcessors instance.
	 *
	 * @return the StatProcessors instance
	 */
	StatProcessors getStatProcessor();
	
	/**
	 * Disconnects the specified player with the given reason.
	 *
	 * @param statPlayer The StatPlayer to disconnect.
	 * @param reason     The reason for the disconnection.
	 */
	@ApiStatus.Internal
	void disconnectPlayer(StatPlayer statPlayer, Component reason);

}
