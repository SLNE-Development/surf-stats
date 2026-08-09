package dev.slne.surf.stats.microservice.db

import dev.slne.surf.api.core.util.logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import java.util.*

private val log = logger()

/**
 * Persists [items] while preserving their order within a group.
 *
 * Items sharing a [groupKeyOf] value are processed sequentially so that older
 * values cannot overwrite newer ones. Different groups are processed
 * concurrently up to [concurrency].
 *
 * @param operationName the operation name used in failure log messages
 * @return the UUIDs of players for which at least one item failed to save
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal suspend fun <T> saveGrouped(
    items: List<T>,
    operationName: String,
    concurrency: Int = DEFAULT_CONCURRENCY,
    playerUuidOf: (T) -> UUID,
    groupKeyOf: (T) -> Any,
    save: suspend (T) -> Unit
): Set<UUID> = items
    .groupBy(groupKeyOf)
    .values
    .asFlow()
    .flatMapMerge(concurrency = concurrency) { group ->
        flow {
            for (item in group) {
                val playerUuid = playerUuidOf(item)

                val failed = runCatching { save(item) }
                    .onFailure { exception ->
                        log.atSevere()
                            .withCause(exception)
                            .log("Failed to save $operationName for player $playerUuid")
                    }
                    .isFailure

                if (failed) {
                    emit(playerUuid)
                }
            }
        }
    }
    .toSet()
