package dev.slne.surf.stats.api.model

import dev.slne.surf.api.core.serializer.java.datetime.datetime.instant.SerializableInstant
import kotlinx.serialization.Serializable

/**
 * A single criterion of an advancement that the player has been awarded.
 *
 * Criterion names are free-form strings chosen by the advancement author
 * (`has_log`, `in_bed`, `minecraft:plains`) and are deliberately **not**
 * modelled as a [net.kyori.adventure.key.Key] — datapacks are not required to
 * use valid resource locations.
 *
 * @property awardedAt when the criterion was awarded, or `null` when the
 *   timestamp in the source file could not be parsed. The criterion still
 *   counts as awarded in that case.
 */
@Serializable
data class AdvancementCriterion(
    val name: String,
    val awardedAt: SerializableInstant? = null
)
