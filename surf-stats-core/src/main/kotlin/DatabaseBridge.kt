package dev.slne.surf.stats.core

import dev.slne.surf.surfapi.core.api.util.requiredService
import java.nio.file.Path

val databaseBridge = requiredService<DatabaseBridge>()

interface DatabaseBridge {
    suspend fun initialize(path: Path)
    fun disconnect()
}