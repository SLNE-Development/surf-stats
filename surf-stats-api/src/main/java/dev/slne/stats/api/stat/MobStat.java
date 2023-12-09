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
 * The type Mob stat.
 */
@Entity(name = "MobStat")
@Table(name = "stats_mobs")
public class MobStat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@JdbcTypeCode(Types.BINARY)
	@Column(name = "stat_owner", nullable = false, length = 16)
	private UUID statOwner;

	@Column(name = "server", nullable = false)
	private String server;

	@Column(name = "mob_key", nullable = false)
	private String mobKey;

	@Column(name = "killed", nullable = false)
	private Long killed;

	@Column(name = "killed_by", nullable = false)
	private Long killedBy;

	/**
	 * Instantiates a new Mob stat.
	 */
	public MobStat() {
	}

	/**
	 * Instantiates a new Mob stat.
	 *
	 * @param statOwner the stat owner
	 * @param server    the server
	 * @param mobKey    the mob key
	 * @param killed    the killed
	 * @param killedBy  the killed by
	 */
	public MobStat(UUID statOwner, String server, String mobKey, Long killed, Long killedBy) {
		this.statOwner = statOwner;
		this.server = server;
		this.mobKey = mobKey;
		this.killed = killed;
		this.killedBy = killedBy;
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
			.append("statOwner", statOwner)
			.append("server", server)
			.append("mobKey", mobKey)
			.append("killed", killed)
			.append("killedBy", killedBy)
			.toString();
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
	 * Stat owner string.
	 *
	 * @return the string
	 */
	public UUID statOwner() {
		return statOwner;
	}

	/**
	 * Mob key string.
	 *
	 * @return the string
	 */
	public String mobKey() {
		return mobKey;
	}

	/**
	 * Killed long.
	 *
	 * @return the long
	 */
	public Long killed() {
		return killed;
	}

	/**
	 * Killed mob stat.
	 *
	 * @param killed the killed
	 *
	 * @return the mob stat
	 */
	public MobStat killed(Long killed) {
		this.killed = killed;
		return this;
	}

	/**
	 * Killed by long.
	 *
	 * @return the long
	 */
	public Long killedBy() {
		return killedBy;
	}

	/**
	 * Killed by mob stat.
	 *
	 * @param killedBy the killed by
	 *
	 * @return the mob stat
	 */
	public MobStat killedBy(Long killedBy) {
		this.killedBy = killedBy;
		return this;
	}

}