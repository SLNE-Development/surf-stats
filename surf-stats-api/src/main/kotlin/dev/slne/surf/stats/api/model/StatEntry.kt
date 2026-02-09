package dev.slne.surf.stats.api.model

/**
 * Represents a single statistic entry matching the database schema.
 * Maps to: player_stats table (categoryName, statKeyName, value)
 */
data class StatEntry(
    val category: String,  // e.g., "minecraft:mined"
    val key: String,       // e.g., "minecraft:stone"
    val value: Long
)
