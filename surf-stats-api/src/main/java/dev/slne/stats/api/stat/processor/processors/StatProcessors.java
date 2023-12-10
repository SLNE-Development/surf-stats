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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The StatProcessors class handles the processing of various player statistics.
 * It contains different processors for different types of stats, such as General, Item, and Mob statistics.
 */
public final class StatProcessors {

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
	 * The StatProcessors class handles the processing of various player statistics.
	 * It contains different processors for different types of stats, such as General, Item, and Mob statistics.
	 */
	@ApiStatus.Internal
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
	 * Processes the statistics for a given player.
	 *
	 * @param statPlayer the player for which the statistics are processed
	 * @param statFile the player's stat file
	 */
	@ApiStatus.Internal
	public void processStats(StatPlayer statPlayer, @NotNull PlayerStatFile statFile) {
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
	 * Processes the statistics for a given player.
	 *
	 * @param statPlayer the player for which the statistics are processed
	 * @param statsJson the JSON string containing the player's stat file
	 */
	@ApiStatus.Internal
	public void processStats(StatPlayer statPlayer, String statsJson) {
		PlayerStatFile.Reader reader = StatsApi.getInstance().getPlayerStatFileReader();
		PlayerStatFile statFile = reader.read(statsJson);

		processStats(statPlayer, statFile);
	}
}
