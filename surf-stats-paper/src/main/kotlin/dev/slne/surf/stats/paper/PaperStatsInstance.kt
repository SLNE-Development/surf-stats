package dev.slne.surf.stats.paper

import com.google.auto.service.AutoService
import dev.slne.surf.stats.core.client.StatsInstance

@AutoService(StatsInstance::class)
class PaperStatsInstance : StatsInstance() {
    override val dataPath get() = plugin.dataPath
}