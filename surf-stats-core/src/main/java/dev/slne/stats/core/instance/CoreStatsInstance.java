package dev.slne.stats.core.instance;

import com.google.gson.Gson;
import dev.slne.stats.api.instance.StatsInstance;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.player.StatPlayerManager;
import dev.slne.stats.api.stat.file.PlayerStatFile;
import dev.slne.stats.api.stat.processor.processors.StatProcessors;
import dev.slne.stats.core.player.CoreStatPlayerManager;
import dev.slne.stats.core.spring.StatsSpringApplication;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.UUID;

/**
 * The CoreStatsInstance class is an abstract class that implements the StatsInstance interface.
 * It provides the core functionality and common methods for managing statistics in an application.
 */
@ApiStatus.Internal
public abstract class CoreStatsInstance implements StatsInstance {

	private final ClassLoader classLoader;
	private File statFolder;

	private ConfigurableApplicationContext context;
	private StatPlayerManager statPlayerManager;

	private StatProcessors statProcessors;
	private PlayerStatFile.Reader playerStatFileReader;

	/**
	 * Constructs a new CoreStatsInstance with the provided class loader.
	 *
	 * @param classLoader the class loader to be used with the CoreStatsInstance
	 */
	@Contract(pure = true)
	public CoreStatsInstance(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void onLoad() {
		this.context = StatsSpringApplication.run(classLoader);

		this.statProcessors = new StatProcessors();
		this.statPlayerManager = new CoreStatPlayerManager();
		this.playerStatFileReader = new PlayerStatFile.Reader(new Gson());
	}

	@Override
	public void onEnable() {
	}

	@Override
	public void onDisable() {
	}

	@Override
	public StatPlayer getStatPlayer(UUID uuid) {
		return statPlayerManager.getStatPlayer(uuid).orElseGet(() -> statPlayerManager.createStatPlayer(uuid));
	}

	@Override
	public StatPlayerManager getStatPlayerManager() {
		return statPlayerManager;
	}

	@Override
	public PlayerStatFile.Reader getPlayerStatFileReader() {
		return playerStatFileReader;
	}

	@Override
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

	@Override
	public File getStatFolder() {
		return statFolder;
	}

	@Override
	public void setStatFolder(File statFolder) {
		this.statFolder = statFolder;
	}

	@Override
	public StatProcessors getStatProcessor() {
		return statProcessors;
	}
}
