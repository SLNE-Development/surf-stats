package dev.slne.surf.stats.api.model

import java.util.UUID

/**
 * Represents player statistics loaded from the filesystem.
 * Maps to: players table + player_stats entries
 */
data class PlayerStats(
    val uuid: UUID,
    val name: String,
    val dataVersion: Int,
    val stats: List<StatEntry>
) {
    /**
     * Gets all stats for a specific category.
     */
    fun getByCategory(category: String): List<StatEntry> =
        stats.filter { it.category == category }

    /**
     * Gets a specific stat value.
     */
    fun getStat(category: String, key: String): Long? =
        stats.find { it.category == category && it.key == key }?.value

    /**
     * Gets all unique categories in this player's stats.
     */
    fun categories(): Set<String> =
        stats.map { it.category }.toSet()

    /**
     * Gets all unique stat keys across all categories.
     */
    fun statKeys(): Set<String> =
        stats.map { it.key }.toSet()

    companion object {
        fun empty(uuid: UUID, name: String = "Unknown", dataVersion: Int = 0): PlayerStats =
            PlayerStats(uuid, name, dataVersion, emptyList())
    }
}
