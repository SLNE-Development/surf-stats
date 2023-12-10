package dev.slne.stats.api.stat;

import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Types;
import java.util.UUID;

/**
 * The ItemStat class represents the statistics of an item.
 */
@ApiStatus.NonExtendable
@Entity
@Table(name = "stats_items")
public class ItemStat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id = -1L;

	@JdbcTypeCode(Types.BINARY)
	@Column(name = "stat_owner", nullable = false, length = 16)
	private UUID statOwner;

	@Column(name = "server", nullable = false)
	private String server;

	@Column(name = "item_key", nullable = false)
	private String itemKey;

	@Column(name = "times_mined", nullable = false)
	private Long timesMined;

	@Column(name = "times_broken", nullable = false)
	private Long timesBroken;

	@Column(name = "times_crafted", nullable = false)
	private Long timesCrafted;

	@Column(name = "times_used", nullable = false)
	private Long timesUsed;

	@Column(name = "times_picked_up", nullable = false)
	private Long timesPickedUp;

	@Column(name = "times_dropped", nullable = false)
	private Long timesDropped;

	/**
	 * Creates a new ItemStat object with default values for all fields.
	 *
	 * @param statOwner the UUID of the stat owner
	 * @param server    the server
	 * @param itemKey   the item key
	 * @return a new ItemStat object with default values
	 */
	@Contract(value = "_, _, _ -> new", pure = true)
	public static @NotNull ItemStat empty(UUID statOwner, String server, String itemKey) {
		return new ItemStat(statOwner, server, itemKey, 0L, 0L, 0L, 0L, 0L, 0L);
	}

	/**
	 * Represents statistics for an item.
	 */
	@ApiStatus.Internal
	@Contract(pure = true)
	protected ItemStat() {
	}

	/**
	 * Creates a new ItemStat object with the provided values.
	 *
	 * @param statOwner     the UUID of the stat owner
	 * @param server        the server
	 * @param itemKey       the item key
	 * @param timesMined    the number of times the item has been mined
	 * @param timesBroken   the number of times the item has been broken
	 * @param timesCrafted  the number of times the item has been crafted
	 * @param timesUsed     the number of times the item has been used
	 * @param timesPickedUp the number of times the item has been picked up
	 * @param timesDropped  the number of times the item has been dropped
	 */
	public ItemStat(UUID statOwner, String server, String itemKey, Long timesMined, Long timesBroken, Long timesCrafted,
					Long timesUsed,
					Long timesPickedUp, Long timesDropped) {
		this.statOwner = statOwner;
		this.server = server;
		this.itemKey = itemKey;
		this.timesMined = timesMined;
		this.timesBroken = timesBroken;
		this.timesCrafted = timesCrafted;
		this.timesUsed = timesUsed;
		this.timesPickedUp = timesPickedUp;
		this.timesDropped = timesDropped;
	}

	/**
	 * Retrieves the id of the ItemStat object.
	 *
	 * @return the id of the ItemStat object
	 */
	public Long id() {
		return id;
	}

	/**
	 * Returns the UUID of the stat owner.
	 *
	 * @return the UUID of the stat owner
	 */
	public UUID statOwner() {
		return statOwner;
	}

	/**
	 * Returns the item key of the {@link ItemStat} object.
	 *
	 * @return the item key of the {@link ItemStat} object
	 */
	public String itemKey() {
		return itemKey;
	}

	/**
	 * Returns the number of times the item has been mined.
	 *
	 * @return the number of times the item has been mined
	 */
	public Long timesMined() {
		return timesMined;
	}

	/**
	 * Sets the number of times the item has been mined for this ItemStat object.
	 *
	 * @param timesMined the number of times the item has been mined
	 * @return the updated ItemStat object
	 */
	public ItemStat timesMined(Long timesMined) {
		this.timesMined = timesMined;

		return this;
	}

	/**
	 * Returns the number of times the item has been broken.
	 *
	 * @return the number of times the item has been broken
	 */
	public Long timesBroken() {
		return timesBroken;
	}

	/**
	 * Sets the number of times the item has been broken and returns the updated ItemStat object.
	 *
	 * @param timesBroken the number of times the item has been broken
	 * @return the updated ItemStat object
	 */
	public ItemStat timesBroken(Long timesBroken) {
		this.timesBroken = timesBroken;

		return this;
	}

	/**
	 * Returns the number of times the item has been crafted.
	 *
	 * @return the number of times the item has been crafted
	 */
	public Long timesCrafted() {
		return timesCrafted;
	}

	/**
	 * Sets the number of times the item has been crafted.
	 *
	 * @param timesCrafted the number of times the item has been crafted
	 * @return the modified ItemStat object
	 */
	public ItemStat timesCrafted(Long timesCrafted) {
		this.timesCrafted = timesCrafted;

		return this;
	}

	/**
	 * Returns the number of times the item has been used.
	 *
	 * @return the number of times the item has been used
	 */
	public Long timesUsed() {
		return timesUsed;
	}

	/**
	 * Updates the number of times the item has been used.
	 *
	 * @param timesUsed the number of times the item has been used
	 * @return the modified ItemStat object
	 */
	public ItemStat timesUsed(Long timesUsed) {
		this.timesUsed = timesUsed;

		return this;
	}

	/**
	 * Returns the number of times the item has been picked up.
	 *
	 * @return the number of times the item has been picked up
	 */
	public Long timesPickedUp() {
		return timesPickedUp;
	}

	/**
	 * Sets the number of times the item has been picked up.
	 *
	 * @param timesPickedUp the number of times the item has been picked up
	 * @return the updated ItemStat object
	 */
	public ItemStat timesPickedUp(Long timesPickedUp) {
		this.timesPickedUp = timesPickedUp;

		return this;
	}

	/**
	 * Retrieves the number of times the item has been dropped.
	 *
	 * @return The number of times the item has been dropped.
	 */
	public Long timesDropped() {
		return timesDropped;
	}

	/**
	 * Sets the number of times the item has been dropped.
	 *
	 * @param timesDropped the number of times the item has been dropped
	 * @return the ItemStat object with the updated timesDropped value
	 */
	public ItemStat timesDropped(Long timesDropped) {
		this.timesDropped = timesDropped;

		return this;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", id)
			.append("statOwner", statOwner)
			.append("server", server)
			.append("itemKey", itemKey)
			.append("timesMined", timesMined)
			.append("timesBroken", timesBroken)
			.append("timesCrafted", timesCrafted)
			.append("timesUsed", timesUsed)
			.append("timesPickedUp", timesPickedUp)
			.append("timesDropped", timesDropped)
			.toString();
	}
}