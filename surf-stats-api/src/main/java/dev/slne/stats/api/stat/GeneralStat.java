package dev.slne.stats.api.stat;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.UUID;

/**
 * The GeneralStat class represents a general statistical data point.
 * It is used to store information about a specific statistic for a given key and owner.
 */
public class GeneralStat {

	@JsonProperty("id")
	private Long id;

	@JsonProperty("general_key")
	private String generalKey;

	@JsonProperty("server")
	private String server;

	@JsonProperty("stat_owner")
	private UUID statOwner;

	@JsonProperty("stat_value")
	private Long statValue;

	/**
	 * The GeneralStat class represents a general statistical data point.
	 * It is used to store information about a specific statistic for a given key and owner.
	 */
	public GeneralStat() {
	}

	/**
	 * Constructs a new GeneralStat object with the specified parameters.
	 *
	 * @param statOwner  the UUID of the stat owner
	 * @param server     the server name
	 * @param generalKey the general key
	 * @param statValue  the stat value
	 */
	public GeneralStat(UUID statOwner, String server, String generalKey, Long statValue) {
		this.generalKey = generalKey;
		this.server = server;
		this.statOwner = statOwner;
		this.statValue = statValue;
	}

	/**
	 * Gets id.
	 *
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Returns the server name associated with this GeneralStat object.
	 *
	 * @return the server name
	 */
	public String getServer() {
		return server;
	}

	/**
	 * Retrieves the general key associated with this GeneralStat object.
	 *
	 * @return the general key
	 */
	public String getGeneralKey() {
		return generalKey;
	}

	/**
	 * Retrieves the UUID of the statistic owner.
	 *
	 * @return the UUID of the statistic owner
	 */
	public UUID getStatOwner() {
		return statOwner;
	}

	/**
	 * Retrieves the value of the stat.
	 *
	 * @return the value of the stat
	 */
	public Long getStatValue() {
		return statValue;
	}

	/**
	 * Sets the value of the stat for the GeneralStat object.
	 *
	 * @param statValue the new value for the stat
	 *
	 * @return the updated GeneralStat object
	 */
	public GeneralStat setStatValue(Long statValue) {
		this.statValue = statValue;
		return this;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
