package dev.slne.stats.api.stat;

import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.sql.Types;
import java.util.UUID;

/**
 * The MobStat class represents a statistic of a mob.
 */
@ApiStatus.NonExtendable
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
	 * The MobStat class represents a statistic of a mob.
	 */
	@Contract(pure = true)
	@ApiStatus.Internal
	protected MobStat() {
	}

	/**
	 * Creates a new instance of the MobStat class with the given parameters.
	 *
	 * @param statOwner The UUID of the stat owner.
	 * @param server    The server name where the mob was killed.
	 * @param mobKey    The key representing the mob.
	 * @param killed    The number of times the mob was killed.
	 * @param killedBy  The number of times the mob was killed by the player.
	 */
	@Contract(pure = true)
	public MobStat(UUID statOwner, String server, String mobKey, Long killed, Long killedBy) {
		this.statOwner = statOwner;
		this.server = server;
		this.mobKey = mobKey;
		this.killed = killed;
		this.killedBy = killedBy;
	}

	/**
	 * Returns the ID of the MobStat.
	 *
	 * @return The ID of the MobStat.
	 */
	public Long id() {
		return id;
	}

	/**
	 * Returns the UUID of the stat owner.
	 *
	 * @return The UUID of the stat owner.
	 */
	public UUID statOwner() {
		return statOwner;
	}

	/**
	 * Retrieves the key representing the mob.
	 *
	 * @return The mob key.
	 */
	public String mobKey() {
		return mobKey;
	}

	/**
	 * Returns the number of times the mob was killed.
	 *
	 * @return The number of times the mob was killed.
	 */
	public Long killed() {
		return killed;
	}

	/**
	 * Sets the number of times the mob was killed and returns the updated MobStat object.
	 *
	 * @param killed The number of times the mob was killed.
	 * @return The updated MobStat object.
	 */
	public MobStat killed(Long killed) {
		this.killed = killed;

		return this;
	}

	/**
	 * Returns the number of times the mob was killed by the player.
	 *
	 * @return The number of times the mob was killed by the player.
	 */
	public Long killedBy() {
		return killedBy;
	}

	/**
	 * Sets the number of times the mob was killed by the player.
	 *
	 * @param killedBy The number of times the mob was killed by the player.
	 * @return The updated MobStat object.
	 */
	public MobStat killedBy(Long killedBy) {
		this.killedBy = killedBy;

		return this;
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

}