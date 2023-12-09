package dev.slne.stats.api.stat.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import dev.slne.stats.api.StatsApi;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The type Player stat file.
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
	 * Uuid uuid.
	 *
	 * @return the uuid
	 */
	public UUID uuid() {
		return uuid;
	}

	/**
	 * Data version string.
	 *
	 * @return the string
	 */
	public String dataVersion() {
		return dataVersion;
	}

	/**
	 * Custom map.
	 *
	 * @return the map
	 */
	public Map<String, Long> custom() {
		return custom;
	}

	/**
	 * Used map.
	 *
	 * @return the map
	 */
	public Map<String, Long> used() {
		return used;
	}

	/**
	 * Dropped map.
	 *
	 * @return the map
	 */
	public Map<String, Long> dropped() {
		return dropped;
	}

	/**
	 * Crafted map.
	 *
	 * @return the map
	 */
	public Map<String, Long> crafted() {
		return crafted;
	}

	/**
	 * Mined map.
	 *
	 * @return the map
	 */
	public Map<String, Long> mined() {
		return mined;
	}

	/**
	 * Picked up map.
	 *
	 * @return the map
	 */
	public Map<String, Long> pickedUp() {
		return pickedUp;
	}

	/**
	 * Broken map.
	 *
	 * @return the map
	 */
	public Map<String, Long> broken() {
		return broken;
	}

	/**
	 * Killed map.
	 *
	 * @return the map
	 */
	public Map<String, Long> killed() {
		return killed;
	}

	/**
	 * Killed by map.
	 *
	 * @return the map
	 */
	public Map<String, Long> killedBy() {
		return killedBy;
	}

	/**
	 * Uuid player stat file.
	 *
	 * @param uuid the uuid
	 *
	 * @return the player stat file
	 */
	public PlayerStatFile uuid(UUID uuid) {
		this.uuid = uuid;
		return this;
	}

	/**
	 * Data version player stat file.
	 *
	 * @param dataVersion the data version
	 *
	 * @return the player stat file
	 */
	public PlayerStatFile dataVersion(String dataVersion) {
		this.dataVersion = dataVersion;

		return this;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("uuid", uuid)
			.append("dataVersion", dataVersion)
			.append("custom", custom)
			.append("used", used)
			.append("dropped", dropped)
			.append("crafted", crafted)
			.append("mined", mined)
			.append("pickedUp", pickedUp)
			.append("broken", broken)
			.append("killed", killed)
			.append("killedBy", killedBy)
			.toString();
	}

	/**
	 * The type Reader.
	 */
	public static class Reader {
		private final Gson gson;

		/**
		 * Instantiates a new Reader.
		 *
		 * @param gson the gson
		 */
		public Reader(Gson gson) {
			this.gson = gson;
		}

		/**
		 * Read player stat file.
		 *
		 * @param uuid the uuid
		 *
		 * @return the player stat file
		 *
		 * @throws FileNotFoundException the file not found exception
		 */
		public PlayerStatFile read(UUID uuid) throws FileNotFoundException {
			File playerFile = new File(StatsApi.getInstance().getStatFolder(), uuid.toString() + ".json");
			JsonObject playerStatFile = gson.fromJson(new JsonReader(new FileReader(playerFile)), JsonObject.class);

			return gson.fromJson(playerStatFile.getAsJsonObject("stats"), PlayerStatFile.class)
				.dataVersion(playerStatFile.get("DataVersion").getAsString()).uuid(uuid);
		}
		
		/**
		 * Read player stat file.
		 *
		 * @param json the json
		 *
		 * @return the player stat file
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
