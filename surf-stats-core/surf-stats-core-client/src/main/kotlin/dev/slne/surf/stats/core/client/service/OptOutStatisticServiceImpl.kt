package dev.slne.surf.stats.core.client.service

import dev.slne.surf.stats.api.model.OptOutInfo
import dev.slne.surf.stats.api.model.OptOutType
import dev.slne.surf.stats.core.client.statsInstance
import dev.slne.surf.stats.core.common.packets.GetOptOutRequestPacket
import dev.slne.surf.stats.core.common.packets.SaveOptOutRequestPacket
import java.util.UUID

object OptOutStatisticServiceImpl : OptOutStatisticService {
    override suspend fun getOptOutput(
        playerUuid: UUID,
    ): List<OptOutInfo> {
        return statsInstance.rabbitApi.sendRequest(
            GetOptOutRequestPacket(playerUuid = playerUuid)
        ).value
    }

    override suspend fun toggleOptOut(playerUuid: UUID, categoryName: String, statisticName: String, type: OptOutType) {
        statsInstance.rabbitApi.sendRequest(
            SaveOptOutRequestPacket(
                playerUuid = playerUuid,
                categoryName = categoryName,
                statisticName = statisticName,
                type = type,
            )
        )
    }
}