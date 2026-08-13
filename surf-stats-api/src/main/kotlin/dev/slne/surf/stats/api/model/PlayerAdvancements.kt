package dev.slne.surf.stats.api.model

import dev.slne.surf.api.core.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable

/**
 * One player's complete advancement snapshot on one server.
 *
 * Snapshots are always complete: the microservice replaces everything it has
 * stored for `(playerUuid, serverName)` with this content. An empty snapshot is
 * therefore never sent — see `PlayerAdvancementsRepository`.
 */
@Serializable
data class PlayerAdvancements(
    val playerUuid: SerializableUUID,
    val serverName: String,
    val advancements: List<AdvancementEntry> = emptyList()
) : Collection<AdvancementEntry> {
    override fun contains(element: AdvancementEntry): Boolean = advancements.contains(element)

    override fun containsAll(elements: Collection<AdvancementEntry>): Boolean =
        advancements.containsAll(elements)

    override fun isEmpty(): Boolean = advancements.isEmpty()

    override fun iterator(): Iterator<AdvancementEntry> = advancements.iterator()

    override val size: Int get() = advancements.size
}
