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
 * The type Item stat.
 */
@Entity
@Table(name = "stats_items")
public class ItemStat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

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
	 * Instantiates a new Item stat.
	 */
	public ItemStat() {
	}

	/**
	 * Instantiates a new Item stat.
	 *
	 * @param statOwner     the stat owner
	 * @param server        the server
	 * @param itemKey       the item key
	 * @param timesMined    the times mined
	 * @param timesBroken   the times broken
	 * @param timesCrafted  the times crafted
	 * @param timesUsed     the times used
	 * @param timesPickedUp the times picked up
	 * @param timesDropped  the times dropped
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
			.append("itemKey", itemKey)
			.append("timesMined", timesMined)
			.append("timesBroken", timesBroken)
			.append("timesCrafted", timesCrafted)
			.append("timesUsed", timesUsed)
			.append("timesPickedUp", timesPickedUp)
			.append("timesDropped", timesDropped)
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
	 * Stat owner uuid.
	 *
	 * @return the uuid
	 */
	public UUID statOwner() {
		return statOwner;
	}

	/**
	 * Item key string.
	 *
	 * @return the string
	 */
	public String itemKey() {
		return itemKey;
	}

	/**
	 * Times mined long.
	 *
	 * @return the long
	 */
	public Long timesMined() {
		return timesMined;
	}

	/**
	 * Times mined item stat.
	 *
	 * @param timesMined the times mined
	 *
	 * @return the item stat
	 */
	public ItemStat timesMined(Long timesMined) {
		this.timesMined = timesMined;
		return this;
	}

	/**
	 * Times broken long.
	 *
	 * @return the long
	 */
	public Long timesBroken() {
		return timesBroken;
	}

	/**
	 * Times broken item stat.
	 *
	 * @param timesBroken the times broken
	 *
	 * @return the item stat
	 */
	public ItemStat timesBroken(Long timesBroken) {
		this.timesBroken = timesBroken;
		return this;
	}

	/**
	 * Times crafted long.
	 *
	 * @return the long
	 */
	public Long timesCrafted() {
		return timesCrafted;
	}

	/**
	 * Times crafted item stat.
	 *
	 * @param timesCrafted the times crafted
	 *
	 * @return the item stat
	 */
	public ItemStat timesCrafted(Long timesCrafted) {
		this.timesCrafted = timesCrafted;
		return this;
	}

	/**
	 * Times used long.
	 *
	 * @return the long
	 */
	public Long timesUsed() {
		return timesUsed;
	}

	/**
	 * Times used item stat.
	 *
	 * @param timesUsed the times used
	 *
	 * @return the item stat
	 */
	public ItemStat timesUsed(Long timesUsed) {
		this.timesUsed = timesUsed;
		return this;
	}

	/**
	 * Times picked up long.
	 *
	 * @return the long
	 */
	public Long timesPickedUp() {
		return timesPickedUp;
	}

	/**
	 * Times picked up item stat.
	 *
	 * @param timesPickedUp the times picked up
	 *
	 * @return the item stat
	 */
	public ItemStat timesPickedUp(Long timesPickedUp) {
		this.timesPickedUp = timesPickedUp;
		return this;
	}

	/**
	 * Times dropped long.
	 *
	 * @return the long
	 */
	public Long timesDropped() {
		return timesDropped;
	}

	/**
	 * Times dropped item stat.
	 *
	 * @param timesDropped the times dropped
	 *
	 * @return the item stat
	 */
	public ItemStat timesDropped(Long timesDropped) {
		this.timesDropped = timesDropped;
		return this;
	}

}