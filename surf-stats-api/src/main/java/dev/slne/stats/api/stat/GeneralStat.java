package dev.slne.stats.api.stat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.util.UUID;

/**
 * The type General stat.
 */
@Entity
@Table(name = "stats_general")
public class GeneralStat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

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
	 * Instantiates a new General stat.
	 */
	public GeneralStat() {
	}

	/**
	 * Instantiates a new General stat.
	 *
	 * @param statOwner  the stat owner
	 * @param server     the server
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
	 * Server string.
	 *
	 * @return the string
	 */
	public String server() {
		return server;
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

	/**
	 * Gets server.
	 *
	 * @return the server
	 */
	public String getServer() {
		return server;
	}

	/**
	 * Sets server.
	 *
	 * @param server the server
	 */
	public void setServer(String server) {
		this.server = server;
	}

	/**
	 * Id long.
	 *
	 * @return the long
	 */
	public Long id() {
		return id;
	}

	/**
	 * General key string.
	 *
	 * @return the string
	 */
	public String generalKey() {
		return generalKey;
	}

	/**
	 * Stat owner string.
	 *
	 * @return the string
	 */
	public UUID statOwner() {
		return statOwner;
	}

	/**
	 * Stat value long.
	 *
	 * @return the long
	 */
	public Long statValue() {
		return statValue;
	}

	/**
	 * Stat value general stat.
	 *
	 * @param statValue the stat value
	 *
	 * @return the general stat
	 */
	public GeneralStat statValue(Long statValue) {
		this.statValue = statValue;
		return this;
	}

}