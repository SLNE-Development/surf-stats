package dev.slne.stats.bukkit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.slne.stats.api.StatsApi;
import dev.slne.stats.api.player.StatPlayer;
import dev.slne.stats.api.stat.GeneralStat;
import dev.slne.stats.api.stat.ItemStat;
import dev.slne.stats.api.stat.MobStat;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

public class SaveCommand implements CommandExecutor {


  private static final ComponentLogger LOGGER = ComponentLogger.logger("SaveCommand");

  public SaveCommand(PluginCommand command) {
    command.setExecutor(this);
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
      @NotNull String[] args
  ) {
    final File path = new File(Bukkit.getWorlds().get(0).getWorldFolder() + "/stats");

    CompletableFuture.runAsync(() -> {
      final File[] jsonStatFiles = path.listFiles((dir, name) -> name.endsWith(".json"));

      if (jsonStatFiles == null) {
        LOGGER.error(Component.text("No files found in " + path.getAbsolutePath()));
        return;
      }

      final Gson gson = new Gson();
      final List<StatPlayer> statPlayers = new ArrayList<>();

      for (final File jsonStatFile : jsonStatFiles) {
        final String json;
        final UUID uuid = UUID.fromString(jsonStatFile.getName().replace(".json", ""));

        try {
          json = gson.fromJson(new FileReader(jsonStatFile), JsonObject.class).toString();
        } catch (FileNotFoundException e) {
          LOGGER.error(Component.text("Failed to read " + jsonStatFile.getName(), NamedTextColor.RED), e);
          continue;
        }

        final StatPlayer statPlayer = StatsApi.getStatPlayer(uuid);
        statPlayers.add(statPlayer);

        StatsApi.getInstance().getStatProcessor().processStats(statPlayer, json);
      }

      final List<GeneralStat> generalStats =
          statPlayers.stream().flatMap(statPlayer -> statPlayer.getGeneralStats().stream()).toList();
      final List<ItemStat> itemStats = statPlayers.stream()
          .flatMap(statPlayer -> statPlayer.getItemStats().stream()).toList();
      final List<MobStat> mobStats = statPlayers.stream().flatMap(statPlayer -> statPlayer.getMobStats().stream())
          .toList();

      final File outputPath = StatsBukkitPlugin.getInstance().getDataFolder();
      final File generalStatsFile = new File(outputPath, "general_stats.sql");
      final File itemStatsFile = new File(outputPath, "item_stats.sql");
      final File mobStatsFile = new File(outputPath, "mob_stats.sql");

      if (!outputPath.exists()) {
        outputPath.mkdirs();
      }

      final StringBuilder generalStatsSql =
          new StringBuilder(
              "INSERT INTO `stats_general` (`stat_owner`, `general_key`, `server`, `stat_value`) VALUES ");
      final StringBuilder itemStatsSql =
          new StringBuilder(
              "INSERT INTO `stats_items` (`stat_owner`, `item_key`, `server`, `times_mined`, `times_broken`, `times_crafted`, `times_used`, `times_picked_up`, `times_dropped`) VALUES ");
      final StringBuilder mobStatsSql =
          new StringBuilder(
              "INSERT INTO `stats_mobs` (`stat_owner`, `mob_key`, `server`, `killed`, `killed_by`) VALUES ");

      for (int i = 0; i < generalStats.size(); i++) {
        final GeneralStat generalStat = generalStats.get(i);
        generalStatsSql.append(String.format(
            "('%s', '%s', '%s', %d)",
            generalStat.getStatOwner().toString(),
            generalStat.getGeneralKey(),
            generalStat.getServer(),
            generalStat.getStatValue()
        ));

        if (i != generalStats.size() - 1) {
          generalStatsSql.append(", ");
        }
      }

      for (int i = 0; i < itemStats.size(); i++) {
        final ItemStat itemStat = itemStats.get(i);
        itemStatsSql.append(String.format(
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
        ));

        if (i != itemStats.size() - 1) {
          itemStatsSql.append(", ");
        }
      }

      for (int i = 0; i < mobStats.size(); i++) {
        final MobStat mobStat = mobStats.get(i);
        mobStatsSql.append(String.format(
            "('%s', '%s', '%s', %d, %d)",
            mobStat.getStatOwner().toString(),
            mobStat.getMobKey(),
            mobStat.getServer(),
            mobStat.getKilled(),
            mobStat.getKilledBy()
        ));

        if (i != mobStats.size() - 1) {
          mobStatsSql.append(", ");
        }
      }

      generalStatsSql.append(";");
      itemStatsSql.append(";");
      mobStatsSql.append(";");

      // write to files
      writeAsyncToFile(generalStatsFile, generalStatsSql.toString());
      writeAsyncToFile(itemStatsFile, itemStatsSql.toString());
      writeAsyncToFile(mobStatsFile, mobStatsSql.toString());
    });





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



//    if (generalStatsFile.exists()) {
//      generalStatsFile.delete();
//    }
//
//    if (itemStatsFile.exists()) {
//      itemStatsFile.delete();
//    }
//
//    if (mobStatsFile.exists()) {
//      mobStatsFile.delete();
//    }

//    try {
//      generalStatsFile.createNewFile();
//      itemStatsFile.createNewFile();
//      mobStatsFile.createNewFile();
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }







    return true;
  }

  private static void writeAsyncToFile(File file, String content) {
    final ByteBuffer buffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));

    try (final AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(file.toPath(),
        StandardOpenOption.WRITE, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)) {

      fileChannel.write(buffer, 0, null, new CompletionHandler<>() {
        @Override
        public void completed(Integer result, Object attachment) {
          LOGGER.info(Component.text("Wrote " + result + " bytes to " + file.getName()));
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
          LOGGER.error(Component.text("Failed to write to " + file.getName()), exc);
        }
      });

    } catch (IOException e) {
      LOGGER.error(Component.text("Failed to write to " + file.getName()), e);
      throw new RuntimeException(e);
    }
  }
}
