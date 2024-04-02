package dev.slne.stats.api.stat;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

/**
 * The GeneralStat class represents a general statistical data point.
 * It is used to store information about a specific statistic for a given key and owner.
 */
@ApiStatus.NonExtendable
public class GeneralStat {

	private String generalKey;
	private String server;
	private UUID statOwner;
	private Long statValue;

	/**
	 * The GeneralStat class represents a general statistical data point.
	 * It is used to store information about a specific statistic for a given key and owner.
	 */
	@ApiStatus.Internal
	protected GeneralStat() {
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
	 * Returns the server name associated with this GeneralStat object.
	 *
	 * @return the server name
	 */
	public String server() {
		return server;
	}

	/**
	 * Retrieves the general key associated with this GeneralStat object.
	 *
	 * @return the general key
	 */
	public String generalKey() {
		return generalKey;
	}

	/**
	 * Retrieves the UUID of the statistic owner.
	 *
	 * @return the UUID of the statistic owner
	 */
	public UUID statOwner() {
		return statOwner;
	}

	/**
	 * Retrieves the value of the stat.
	 *
	 * @return the value of the stat
	 */
	public Long statValue() {
		return statValue;
	}

	/**
	 * Sets the value of the stat for the GeneralStat object.
	 *
	 * @param statValue the new value for the stat
	 *
	 * @return the updated GeneralStat object
	 */
	public GeneralStat statValue(Long statValue) {
		this.statValue = statValue;
		return this;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
