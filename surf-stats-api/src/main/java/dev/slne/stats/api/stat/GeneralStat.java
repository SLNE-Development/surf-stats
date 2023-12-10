package dev.slne.stats.api.stat;

import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.sql.Types;
import java.util.UUID;

/**
 * The GeneralStat class represents a general statistical data point.
 * It is used to store information about a specific statistic for a given key and owner.
 */
@ApiStatus.NonExtendable
@Entity
@Table(name = "stats_general")
public class GeneralStat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id = -1L;

	@Column(name = "general_key", nullable = false)
	private String generalKey;

	@Column(name = "server", nullable = false)
	private String server;

	@JdbcTypeCode(Types.BINARY)
	@Column(name = "stat_owner", nullable = false, length = 16)
	private UUID statOwner;

	@Column(name = "stat_value", nullable = false)
	private Long statValue;

	/**
	 * The GeneralStat class represents a general statistical data point.
	 * It is used to store information about a specific statistic for a given key and owner.
	 */
	@ApiStatus.Internal
	@Contract(pure = true)
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
	@Contract(pure = true)
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
	public String getServer() {
		return server;
	}

	/**
	 * Sets the server name for this GeneralStat object.
	 *
	 * @param server the server name to be set
	 */
	public void setServer(String server) {
		this.server = server;
	}

	/**
	 * Returns the ID of the GeneralStat object.
	 *
	 * @return the ID of the GeneralStat object
	 */
	public Long id() {
		return id;
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
	 * @return the updated GeneralStat object
	 */
	public GeneralStat statValue(Long statValue) {
		this.statValue = statValue;
		return this;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", id)
			.append("generalKey", generalKey)
			.append("server", server)
			.append("statOwner", statOwner)
			.append("statValue", statValue)
			.toString();
	}
}