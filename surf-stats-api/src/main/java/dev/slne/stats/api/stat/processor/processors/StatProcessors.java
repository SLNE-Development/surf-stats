package dev.slne.stats.api.stat.processor.processors;

import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.file.PlayerStatFile;
import dev.slne.stats.api.stat.processor.processors.general.GeneralStatProcessor;
import dev.slne.stats.api.stat.processor.processors.item.BrokenStatProcessor;
import dev.slne.stats.api.stat.processor.processors.item.CraftedStatProcessor;
import dev.slne.stats.api.stat.processor.processors.item.DroppedStatProcessor;
import dev.slne.stats.api.stat.processor.processors.item.MinedStatProcessor;
import dev.slne.stats.api.stat.processor.processors.item.PickedUpStatProcessor;
import dev.slne.stats.api.stat.processor.processors.item.UsedStatProcessor;
import dev.slne.stats.api.stat.processor.processors.mob.KilledByStatProcessor;
import dev.slne.stats.api.stat.processor.processors.mob.KilledStatProcessor;

/**
 * The type Stat processors.
 */
public class StatProcessors {

	// General
	private final GeneralStatProcessor generalStatProcessor;

	// Item
	private final BrokenStatProcessor brokenStatProcessor;
	private final CraftedStatProcessor craftedStatProcessor;
	private final DroppedStatProcessor droppedStatProcessor;
	private final PickedUpStatProcessor pickedUpStatProcessor;
	private final UsedStatProcessor usedStatProcessor;
	private final MinedStatProcessor minedStatProcessor;

	// Mob
	private final KilledStatProcessor killedStatProcessor;
	private final KilledByStatProcessor killedByStatProcessor;

	/**
	 * Instantiates a new Stat processors.
	 */
	public StatProcessors() {
		// General
		generalStatProcessor = new GeneralStatProcessor();

		// Item
		brokenStatProcessor = new BrokenStatProcessor();
		craftedStatProcessor = new CraftedStatProcessor();
		droppedStatProcessor = new DroppedStatProcessor();
		pickedUpStatProcessor = new PickedUpStatProcessor();
		usedStatProcessor = new UsedStatProcessor();
		minedStatProcessor = new MinedStatProcessor();

		// Mob
		killedStatProcessor = new KilledStatProcessor();
		killedByStatProcessor = new KilledByStatProcessor();
	}

	/**
	 * Process stats.
	 *
	 * @param statPlayer the stat player
	 * @param statFile   the stat file
	 */
	public void processStats(StatPlayer statPlayer, PlayerStatFile statFile) {
		// General
		generalStatProcessor.processStats(statPlayer, statFile.custom());

		// Item
		brokenStatProcessor.processStats(statPlayer, statFile.broken());
		craftedStatProcessor.processStats(statPlayer, statFile.crafted());
		droppedStatProcessor.processStats(statPlayer, statFile.dropped());
		pickedUpStatProcessor.processStats(statPlayer, statFile.pickedUp());
		usedStatProcessor.processStats(statPlayer, statFile.used());
		minedStatProcessor.processStats(statPlayer, statFile.mined());

		// Mob
		killedStatProcessor.processStats(statPlayer, statFile.killed());
		killedByStatProcessor.processStats(statPlayer, statFile.killedBy());
	}

	/**
	 * Process stats.
	 *
	 * @param statPlayer the stat player
	 * @param statsJson  the stats json
	 */
	public void processStats(StatPlayer statPlayer, String statsJson) {
		PlayerStatFile.Reader reader = StatsApi.getInstance().getPlayerStatFileReader();
		PlayerStatFile statFile = reader.read(statsJson);

		processStats(statPlayer, statFile);
	}
}
