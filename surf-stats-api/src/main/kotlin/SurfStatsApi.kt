import dev.slne.surf.surfapi.core.api.util.requiredService
import java.util.*

val surfStatsApi = requiredService<SurfStatsApi>()

interface SurfStatsApi {

    /**
     * Saves the statistic of a specific player
     */
    suspend fun saveStatistics(playerUuid: UUID)

}