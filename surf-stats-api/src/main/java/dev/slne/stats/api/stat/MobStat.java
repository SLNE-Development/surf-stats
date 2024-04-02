package dev.slne.stats.api.stat;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

/**
 * The MobStat class represents a statistic of a mob.
 */
@ApiStatus.NonExtendable
public class MobStat {

	private UUID statOwner;
	private String server;
	private String mobKey;
	private Long killed;
	private Long killedBy;

	/**
	 * The MobStat class represents a statistic of a mob.
	 */
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
	public MobStat(UUID statOwner, String server, String mobKey, Long killed, Long killedBy) {
		this.statOwner = statOwner;
		this.server = server;
		this.mobKey = mobKey;
		this.killed = killed;
		this.killedBy = killedBy;
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
	 *
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
	 *
	 * @return The updated MobStat object.
	 */
	public MobStat killedBy(Long killedBy) {
		this.killedBy = killedBy;

		return this;
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
		return ToStringBuilder.reflectionToString(this);
	}
}