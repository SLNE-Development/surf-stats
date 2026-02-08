import java.util.UUID

interface SurfStatsFileService {

    /**
     * Reads the statistics file on the file system
     */
    suspend fun loadStatistics(playerUuid: UUID)

}