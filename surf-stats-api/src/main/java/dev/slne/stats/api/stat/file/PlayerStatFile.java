package dev.slne.stats.api.stat.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import dev.slne.stats.api.StatsApi;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The PlayerStatFile class represents a player's statistics file.
 */
public class PlayerStatFile {

	@JsonIgnore
	private transient UUID uuid;

	@SerializedName("DataVersion")
	private String dataVersion;


	//	General
	@SerializedName("minecraft:custom")
	private Map<String, Long> custom;

	//	Item
	@SerializedName("minecraft:used")
	private Map<String, Long> used;

	@SerializedName("minecraft:dropped")
	private Map<String, Long> dropped;

	@SerializedName("minecraft:crafted")
	private Map<String, Long> crafted;

	@SerializedName("minecraft:mined")
	private Map<String, Long> mined;

	@SerializedName("minecraft:picked_up")
	private Map<String, Long> pickedUp;

	@SerializedName("minecraft:broken")
	private Map<String, Long> broken;

	// Mob
	@SerializedName("minecraft:killed")
	private Map<String, Long> killed;

	@SerializedName("minecraft:killed_by")
	private Map<String, Long> killedBy;

	/**
	 * Instantiates a new Player stat file.
	 */
	public PlayerStatFile() {
		// General
		this.custom = Collections.unmodifiableMap(new HashMap<>());

		// Item
		this.used = Collections.unmodifiableMap(new HashMap<>());
		this.dropped = Collections.unmodifiableMap(new HashMap<>());
		this.crafted = Collections.unmodifiableMap(new HashMap<>());
		this.mined = Collections.unmodifiableMap(new HashMap<>());
		this.pickedUp = Collections.unmodifiableMap(new HashMap<>());
		this.broken = Collections.unmodifiableMap(new HashMap<>());

		// Mob
		this.killed = Collections.unmodifiableMap(new HashMap<>());
		this.killedBy = Collections.unmodifiableMap(new HashMap<>());
	}

	/**
	 * Retrieves the UUID associated with the player stat file.
	 *
	 * @return the UUID associated with the player stat file
	 */
	public UUID uuid() {
		return uuid;
	}

	/**
	 * Retrieves the data version of the player stat file.
	 *
	 * @return the data version of the player stat file
	 */
	public String dataVersion() {
		return dataVersion;
	}

	/**
	 * Returns the custom map from the PlayerStatFile object.
	 *
	 * @return the custom map from the PlayerStatFile object
	 */
	public Map<String, Long> custom() {
		return custom;
	}

	/**
	 * Retrieves a map of the "used" statistics from the player's stat file.
	 *
	 * @return a map containing the "used" statistics, where the keys are the names of the items and the values are the corresponding usage counts
	 */
	public Map<String, Long> used() {
		return used;
	}

	/**
	 * Returns a map of items dropped by the player.
	 *
	 * @return a map of items dropped
	 */
	public Map<String, Long> dropped() {
		return dropped;
	}

	/**
	 * Retrieves the crafted statistics for a player.
	 *
	 * @return a Map containing the name of each crafted item and the number of times it has been crafted, or an empty Map if no items have been crafted
	 */
	public Map<String, Long> crafted() {
		return crafted;
	}

	/**
	 * Retrieves the map of mined items for the player.
	 *
	 * @return the map representing the mined items, where the keys are the item names and the values are the quantities mined
	 */
	public Map<String, Long> mined() {
		return mined;
	}

	/**
	 * Retrieves the map of items picked up by the player.
	 *
	 * @return a map containing the item names as keys and the number of times they were picked up as values
	 */
	public Map<String, Long> pickedUp() {
		return pickedUp;
	}

	/**
	 * Retrieves the map of broken statistics.
	 *
	 * @return the map of broken statistics
	 */
	public Map<String, Long> broken() {
		return broken;
	}

	/**
	 * This method returns the "killed" map from the PlayerStatFile class.
	 * The "killed" map contains the statistics of the player's kills.
	 *
	 * @return the "killed" map which is a mapping of mob names to the number of times the player has killed them
	 */
	public Map<String, Long> killed() {
		return killed;
	}

	/**
	 * Returns the map of players that killed the current player along with the number of times they killed them.
	 *
	 * @return the map of players that killed the current player along with the number of times they killed them
	 */
	public Map<String, Long> killedBy() {
		return killedBy;
	}

	/**
	 * Sets the UUID of the PlayerStatFile.
	 *
	 * @param uuid the UUID to set
	 *
	 * @return the updated PlayerStatFile object
	 */
	public PlayerStatFile uuid(UUID uuid) {
		this.uuid = uuid;
		return this;
	}

	/**
	 * Sets the data version of the PlayerStatFile.
	 *
	 * @param dataVersion the data version to set
	 *
	 * @return the updated PlayerStatFile object
	 */
	public PlayerStatFile dataVersion(String dataVersion) {
		this.dataVersion = dataVersion;

		return this;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	/**
	 * The Reader class is responsible for reading player stat files.
	 */
	public static class Reader {
		private final Gson gson;

		/**
		 * The Reader class is responsible for reading player stat files.
		 *
		 * @param gson the Gson object used to read the player stat files
		 */
		@Contract(pure = true)
		public Reader(Gson gson) {
			this.gson = gson;
		}

		/**
		 * Reads a player's statistics file.
		 *
		 * @param uuid the UUID of the player whose statistics file needs to be read
		 *
		 * @return the PlayerStatFile object representing the player's statistics file
		 *
		 * @throws FileNotFoundException if the statistics file for the player with the specified UUID cannot be found
		 */
		public PlayerStatFile read(@NotNull UUID uuid) throws FileNotFoundException {
			File playerFile = new File(StatsApi.getInstance().getStatFolder(), uuid + ".json");
			JsonObject playerStatFile = gson.fromJson(new JsonReader(new FileReader(playerFile)), JsonObject.class);

			return gson.fromJson(playerStatFile.getAsJsonObject("stats"), PlayerStatFile.class)
					   .dataVersion(playerStatFile.get("DataVersion").getAsString())
					   .uuid(uuid);
		}

		/**
		 * Reads a player stat file from the provided JSON string.
		 *
		 * @param json the JSON string representing the player stat file
		 *
		 * @return the PlayerStatFile object parsed from the JSON string
		 */
		public PlayerStatFile read(String json) {
			JsonObject playerStatFile = gson.fromJson(json, JsonObject.class);

			return gson.fromJson(playerStatFile.getAsJsonObject("stats"), PlayerStatFile.class)
					   .dataVersion(playerStatFile.get("DataVersion").getAsString());
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this)
				.append("gson", gson)
				.toString();
		}
	}
}
