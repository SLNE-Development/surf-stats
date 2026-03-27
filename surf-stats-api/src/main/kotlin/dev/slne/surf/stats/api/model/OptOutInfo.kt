package dev.slne.surf.stats.api.model

import dev.slne.surf.surfapi.core.api.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key

@Serializable
data class OptOutInfo(
    val playerUuid: SerializableUUID,
    val categoryName: Key,
    val statisticName: Key,
    val type: OptOutType
)