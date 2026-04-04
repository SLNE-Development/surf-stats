package dev.slne.surf.stats.api.model

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable

@Serializable
data class PlayerStats(
    val playerUuid: SerializableUUID,
    val serverName: String,
    val stats: List<StatEntry> = emptyList()
) : Collection<StatEntry> {
    override fun contains(element: StatEntry): Boolean {
        return stats.contains(element)
    }

    override fun containsAll(elements: Collection<StatEntry>): Boolean {
        return stats.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return stats.isEmpty()
    }

    override fun iterator(): Iterator<StatEntry> {
        return stats.iterator()
    }

    override val size: Int get() = stats.size
}