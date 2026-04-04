package dev.slne.surf.stats.api.model

import dev.slne.surf.api.core.serializer.adventure.key.SerializableKey
import kotlinx.serialization.Serializable

/**
 * Represents a single statistic entry matching the database schema.
 * Maps to: player_stats table (categoryName, statKeyName, value)
 */
@Serializable
data class StatEntry(
    val category: SerializableKey,  // e.g., "minecraft:mined"
    val key: SerializableKey,       // e.g., "minecraft:stone"
    val value: Long
)
