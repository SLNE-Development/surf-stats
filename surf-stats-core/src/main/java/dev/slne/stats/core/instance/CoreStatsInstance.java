package dev.slne.stats.core.instance;

import com.google.gson.Gson;
import dev.slne.stats.api.instance.StatsInstance;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.player.StatPlayerManager;
import dev.slne.stats.api.stat.file.PlayerStatFile;
import dev.slne.stats.api.stat.processor.processors.StatProcessors;
import dev.slne.stats.core.player.CoreStatPlayerManager;
import dev.slne.stats.core.spring.StatsSpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.UUID;

/**
 * The type Core stats instance.
 */
public abstract class CoreStatsInstance implements StatsInstance {

	private final ClassLoader classLoader;
	private File statFolder;

	private ConfigurableApplicationContext context;
	private StatPlayerManager statPlayerManager;

	private StatProcessors statProcessors;
	private PlayerStatFile.Reader playerStatFileReader;

	/**
	 * Instantiates a new Core stats instance.
	 *
	 * @param classLoader the class loader
	 */
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
