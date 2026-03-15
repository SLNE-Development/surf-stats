package dev.slne.surf.stats.paper

import com.google.auto.service.AutoService
import dev.slne.surf.stats.api.StatsInstance
import dev.slne.surf.stats.core.AbstractStatsInstance

@AutoService(StatsInstance::class)
class PaperStatsInstance : AbstractStatsInstance() {
    override val dataPath get() = plugin.dataPath
}