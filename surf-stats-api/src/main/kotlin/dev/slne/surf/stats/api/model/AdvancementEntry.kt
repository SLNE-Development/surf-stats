package dev.slne.surf.stats.api.model

import dev.slne.surf.api.core.serializer.adventure.key.SerializableKey
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * A player's progress on one advancement.
 *
 * [completedAt] and [criteriaDone] are computed properties without backing
 * fields, so kotlinx.serialization does not put them on the wire — the
 * microservice derives them when writing.
 */
@Serializable
data class AdvancementEntry(
    val advancement: SerializableKey,
    val done: Boolean,
    val criteria: List<AdvancementCriterion> = emptyList()
) {
    /** The latest criterion timestamp once the advancement is complete, else `null`. */
    val completedAt: Instant?
        get() = if (done) criteria.mapNotNull { it.awardedAt }.maxOrNull() else null

    /** Number of awarded criteria. The total is not present in the source file. */
    val criteriaDone: Int get() = criteria.size
}
