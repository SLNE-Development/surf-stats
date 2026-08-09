package dev.slne.surf.stats.core.client.json

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.stats.api.model.AdvancementCriterion
import dev.slne.surf.stats.api.model.AdvancementEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Parses the Minecraft player advancement file.
 * File location: `<world>/advancements/<uuid>.json`
 *
 * Example structure:
 * ```json
 * {
 *   "minecraft:adventure/adventuring_time": {
 *     "criteria": { "minecraft:plains": "2024-05-15 16:30:56 +0000" },
 *     "done": false
 *   },
 *   "DataVersion": 4189
 * }
 * ```
 *
 * Recipe advancements are dropped: they are a gameplay mechanic rather than a
 * statistic and outnumber real advancements roughly ten to one. Individual
 * malformed entries are skipped so that one bad entry cannot cost a player
 * their entire snapshot.
 */
object AdvancementsJsonModel {
    private val log = logger()

    private const val DATA_VERSION_KEY = "DataVersion"
    private const val RECIPE_PATH_PREFIX = "recipes/"

    // Deliberately no `coerceInputValues`: with it, a JSON null for `criteria` or
    // `done` would be silently replaced by the property default, so a malformed
    // entry would be accepted instead of skipped and logged.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(content: String): List<AdvancementEntry> {
        val root = json.parseToJsonElement(content).jsonObject

        return root.mapNotNull { (rawId, element) ->
            if (rawId == DATA_VERSION_KEY) return@mapNotNull null

            val advancement = runCatching { key(rawId) }.getOrElse {
                log.atWarning().log("Skipping advancement with invalid identifier: %s", rawId)
                return@mapNotNull null
            }

            if (advancement.value().startsWith(RECIPE_PATH_PREFIX)) return@mapNotNull null

            val progress = runCatching { json.decodeFromJsonElement<RawProgress>(element) }
                .getOrElse { error ->
                    log.atWarning()
                        .withCause(error)
                        .log("Skipping malformed advancement entry: %s", rawId)
                    return@mapNotNull null
                }

            AdvancementEntry(
                advancement = advancement,
                done = progress.done,
                criteria = progress.criteria.map { (name, rawTimestamp) ->
                    AdvancementCriterion(
                        name = name,
                        awardedAt = AdvancementTimestamp.parse(rawTimestamp)
                    )
                }
            )
        }
    }

    @Serializable
    private data class RawProgress(
        val criteria: Map<String, String> = emptyMap(),
        val done: Boolean = false
    )
}
