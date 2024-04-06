package dev.slne.stats.bukkit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.GeneralStat;
import dev.slne.stats.api.stat.ItemStat;
import dev.slne.stats.api.stat.MobStat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SaveCommand implements CommandExecutor {

	public SaveCommand(PluginCommand command) {
		command.setExecutor(this);
	}

	@Override
	public boolean onCommand(
		@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args
	) {
		File path = new File(Bukkit.getWorlds().get(0).getWorldFolder() + "/stats");
		File[] jsonStatFiles = path.listFiles((dir, name) -> name.endsWith(".json"));

		Gson gson = new Gson();

		List<StatPlayer> map = new ArrayList<>();

		for (File jsonStatFile : jsonStatFiles) {
			String json;
			UUID uuid = UUID.fromString(jsonStatFile.getName().replace(".json", ""));

			try {
				json = gson.fromJson(new FileReader(jsonStatFile), JsonObject.class).toString();
			} catch (FileNotFoundException e) {
				ComponentLogger.logger(getClass()).error(Component.text("Failed " + uuid, NamedTextColor.RED), e);
				continue;
			}

			StatPlayer statPlayer = StatsApi.getStatPlayer(uuid);
			map.add(statPlayer);

			StatsApi.getInstance().getStatProcessor().processStats(statPlayer, json);
		}

		List<GeneralStat> generalStats =
			map.stream().flatMap(statPlayer -> statPlayer.getGeneralStats().stream()).toList();
		List<ItemStat> itemStats = map.stream().flatMap(statPlayer -> statPlayer.getItemStats().stream()).toList();
		List<MobStat> mobStats = map.stream().flatMap(statPlayer -> statPlayer.getMobStats().stream()).toList();

		/*
		 *CREATE TABLE `stats_general` (
	`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`stat_owner` VARCHAR(36) NOT NULL DEFAULT '' COLLATE 'latin1_swedish_ci',
	`general_key` VARCHAR(255) NOT NULL COLLATE 'latin1_swedish_ci',
	`server` VARCHAR(255) NOT NULL COLLATE 'latin1_swedish_ci',
	`stat_value` BIGINT(20) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `mob_key_stat_owner` (`general_key`, `stat_owner`) USING BTREE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
AUTO_INCREMENT=2647
;

		 */

		/*
		CREATE TABLE `stats_items` (
	`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`stat_owner` VARCHAR(36) NOT NULL DEFAULT '' COLLATE 'latin1_swedish_ci',
	`item_key` VARCHAR(255) NOT NULL COLLATE 'latin1_swedish_ci',
	`server` VARCHAR(255) NOT NULL COLLATE 'latin1_swedish_ci',
	`times_mined` BIGINT(20) NOT NULL DEFAULT '0',
	`times_broken` BIGINT(20) NOT NULL DEFAULT '0',
	`times_crafted` BIGINT(20) NOT NULL DEFAULT '0',
	`times_used` BIGINT(20) NOT NULL DEFAULT '0',
	`times_picked_up` BIGINT(20) NOT NULL DEFAULT '0',
	`times_dropped` BIGINT(20) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `stat_owner_item_key` (`stat_owner`, `item_key`) USING BTREE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
AUTO_INCREMENT=14051
;

		 */
		/*
		CREATE TABLE `stats_mobs` (
	`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
	`stat_owner` VARCHAR(36) NOT NULL DEFAULT '' COLLATE 'latin1_swedish_ci',
	`mob_key` VARCHAR(255) NOT NULL COLLATE 'latin1_swedish_ci',
	`server` VARCHAR(255) NOT NULL COLLATE 'latin1_swedish_ci',
	`killed` BIGINT(20) NOT NULL DEFAULT '0',
	`killed_by` BIGINT(20) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `mob_key_stat_owner` (`mob_key`, `stat_owner`) USING BTREE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
AUTO_INCREMENT=2189
;

		 */

		File outputPath = StatsBukkitPlugin.getInstance().getDataFolder();
		File generalStatsFile = new File(outputPath, "general_stats.sql");
		File itemStatsFile = new File(outputPath, "item_stats.sql");
		File mobStatsFile = new File(outputPath, "mob_stats.sql");

		if (!outputPath.exists()) {
			outputPath.mkdirs();
		}

		if (generalStatsFile.exists()) {
			generalStatsFile.delete();
		}

		if (itemStatsFile.exists()) {
			itemStatsFile.delete();
		}

		if (mobStatsFile.exists()) {
			mobStatsFile.delete();
		}

		try {
			generalStatsFile.createNewFile();
			itemStatsFile.createNewFile();
			mobStatsFile.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String generalStatsSql =
			"INSERT INTO `stats_general` (`stat_owner`, `general_key`, `server`, `stat_value`) VALUES ";
		String itemStatsSql =
			"INSERT INTO `stats_items` (`stat_owner`, `item_key`, `server`, `times_mined`, `times_broken`, `times_crafted`, `times_used`, `times_picked_up`, `times_dropped`) VALUES ";
		String mobStatsSql =
			"INSERT INTO `stats_mobs` (`stat_owner`, `mob_key`, `server`, `killed`, `killed_by`) VALUES ";

		for (int i = 0; i < generalStats.size(); i++) {
			GeneralStat generalStat = generalStats.get(i);
			generalStatsSql += String.format(
				"('%s', '%s', '%s', %d)",
				generalStat.getStatOwner().toString(),
				generalStat.getGeneralKey(),
				generalStat.getServer(),
				generalStat.getStatValue()
			);

			if (i != generalStats.size() - 1) {
				generalStatsSql += ", ";
			}
		}

		for (int i = 0; i < itemStats.size(); i++) {
			ItemStat itemStat = itemStats.get(i);
			itemStatsSql += String.format(
				"('%s', '%s', '%s', %d, %d, %d, %d, %d, %d)",
				itemStat.getStatOwner().toString(),
				itemStat.getItemKey(),
				itemStat.getServer(),
				itemStat.getTimesMined(),
				itemStat.getTimesBroken(),
				itemStat.getTimesCrafted(),
				itemStat.getTimesUsed(),
				itemStat.getTimesPickedUp(),
				itemStat.getTimesDropped()
			);

			if (i != itemStats.size() - 1) {
				itemStatsSql += ", ";
			}
		}

		for (int i = 0; i < mobStats.size(); i++) {
			MobStat mobStat = mobStats.get(i);
			mobStatsSql += String.format(
				"('%s', '%s', '%s', %d, %d)",
				mobStat.getStatOwner().toString(),
				mobStat.getMobKey(),
				mobStat.getServer(),
				mobStat.getKilled(),
				mobStat.getKilledBy()
			);

			if (i != mobStats.size() - 1) {
				mobStatsSql += ", ";
			}
		}

		generalStatsSql += ";";
		itemStatsSql += ";";
		mobStatsSql += ";";

		// write to files

		return true;
	}
}
