package dev.slne.surf.stats.api.model

import dev.slne.surf.surfapi.core.api.serializer.adventure.key.SerializableKey
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.SerializableUUID
import kotlinx.serialization.Serializable

@Serializable
data class OptOutInfo(
    val playerUuid: SerializableUUID,
    val categoryName: SerializableKey,
    val statisticName: SerializableKey,
    val type: OptOutType
)