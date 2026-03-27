package dev.slne.surf.stats.core.client.service

import dev.slne.surf.stats.api.model.OptOutInfo
import dev.slne.surf.stats.api.model.OptOutType
import java.util.UUID

interface OptOutStatisticService {

    suspend fun getOptOutput(playerUuid: UUID): List<OptOutInfo>

    suspend fun toggleOptOut(playerUuid: UUID, categoryName: String, statisticName: String, type: OptOutType)

    companion object : OptOutStatisticService by OptOutStatisticServiceImpl
}