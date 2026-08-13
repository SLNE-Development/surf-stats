package dev.slne.surf.stats.core.client.json

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Parses the criterion timestamps written into `advancements/<uuid>.json`.
 *
 * Vanilla writes `yyyy-MM-dd HH:mm:ss Z` (for example `2024-05-15 16:30:56 +0000`).
 * It could not be verified from the documentation whether newer versions emit
 * ISO-8601 instead, so both are accepted. An unparsable value yields `null`
 * rather than dropping the criterion — the fact that it was awarded still matters.
 */
object AdvancementTimestamp {
    private val VANILLA_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT)

    fun parse(raw: String): Instant? =
        tryParse(raw) { OffsetDateTime.parse(it, VANILLA_FORMAT).toInstant() }
            ?: tryParse(raw) { Instant.parse(it) }
            ?: tryParse(raw) { OffsetDateTime.parse(it).toInstant() }

    private inline fun tryParse(raw: String, parse: (String) -> Instant): Instant? =
        runCatching { parse(raw) }.getOrNull()
}
