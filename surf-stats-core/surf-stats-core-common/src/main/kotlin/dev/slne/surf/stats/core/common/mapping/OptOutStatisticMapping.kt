package dev.slne.surf.stats.core.common.mapping

/**
 * When a player wants to opt out of a statistic,
 * we need to know which statistics to opt out of.
 * In minecraft, play_time is a special case,
 * where we need to opt out of both play_time and total_world_time.
 */
var optOutStatisticMapping = listOf<OptOutStatisticMappingItem>(
    OptOutStatisticMappingItem(
        "minecraft:custom",
        "minecraft:play_time",
        listOf("minecraft:play_time", "minecraft:total_world_time")
    ),
)

data class OptOutStatisticMappingItem(val categoryName: String, val statisticName: String, val items: List<String>)