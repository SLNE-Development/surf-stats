package dev.slne.surf.stats.core.client.json

import dev.slne.surf.stats.api.model.StatEntry
import dev.slne.surf.surfapi.core.api.messages.adventure.key
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * JSON model representing the Minecraft statistics file format.
 * File location: <world>/stats/<uuid>.json
 *
 * Example structure:
 * ```json
 * {
 *   "stats": {
 *     "minecraft:mined": {
 *       "minecraft:stone": 100,
 *       "minecraft:dirt": 50
 *     },
 *     "minecraft:killed": {
 *       "minecraft:zombie": 25
 *     }
 *   },
 *   "DataVersion": 3953
 * }
 * ```
 */
@Serializable
data class StatsJsonModel(
    @SerialName("stats")
    private val stats: Map<String, Map<String, Long>> = emptyMap(),

    @SerialName("DataVersion")
    val dataVersion: Int = 0
) {
    @Transient
    private val statsMap = stats.map { (category, items) ->
        key(category) to items.map { (key, value) ->
            key(key) to value
        }
    }

    /**
     * Converts the nested stats map to a flat list of StatEntry objects.
     * Each entry contains: category (e.g., "minecraft:mined"), key (e.g., "minecraft:stone"), value
     */
    fun toStatEntries(): List<StatEntry> {
        return statsMap.flatMap { (category, items) ->
            items.map { (key, value) ->
                StatEntry(
                    category = category,
                    key = key,
                    value = value
                )
            }
        }
    }

    /**
     * Gets stats for a specific category (e.g., "minecraft:mined", "minecraft:killed").
     */
    fun getCategory(category: String): Map<String, Long> =
        stats[category] ?: emptyMap()

    /**
     * Returns all unique category names.
     */
    fun categoryNames(): Set<String> = stats.keys

    /**
     * Returns all unique stat key names across all categories.
     */
    fun statKeyNames(): Set<String> = stats.values.flatMap { it.keys }.toSet()
}
