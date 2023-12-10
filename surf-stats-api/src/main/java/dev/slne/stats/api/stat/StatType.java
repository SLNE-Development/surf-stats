package dev.slne.stats.api.stat;

import org.jetbrains.annotations.ApiStatus;

/**
 * The StatType enum represents the different types of statistics in the system.
 *
 * @deprecated This class is scheduled for removal in version 1.1.0 as it is no longer used.
 */
@Deprecated(forRemoval = true, since = "1.0.0")
@ApiStatus.ScheduledForRemoval(inVersion = "1.1.0")
public enum StatType {
	/**
	 * The GENERAL variable represents the general type of statistic in the system.
	 *
	 * @see StatType
	 */
	GENERAL,
	/**
	 * The ITEM class represents an item in the system.
	 */
	ITEM,
	/**
	 * The MOB class represents a mobile object in the system.
	 */
	MOB,
	/**
	 * This is a variable named CUSTOM.
	 * It is a member of the StatType enum and represents a custom type of statistics in the system.
	 * The CUSTOM type can be used to define additional statistical categories or specializations as needed.
	 */
	CUSTOM

}
