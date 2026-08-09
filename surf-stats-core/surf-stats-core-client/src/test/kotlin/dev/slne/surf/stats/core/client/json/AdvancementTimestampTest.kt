package dev.slne.surf.stats.core.client.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class AdvancementTimestampTest {

    @Test
    fun `parses the vanilla format`() {
        assertEquals(
            Instant.parse("2024-05-15T16:30:56Z"),
            AdvancementTimestamp.parse("2024-05-15 16:30:56 +0000")
        )
    }

    @Test
    fun `applies the offset of the vanilla format`() {
        assertEquals(
            Instant.parse("2024-05-15T14:30:56Z"),
            AdvancementTimestamp.parse("2024-05-15 16:30:56 +0200")
        )
    }

    @Test
    fun `parses an ISO instant`() {
        assertEquals(
            Instant.parse("2024-05-15T16:30:56Z"),
            AdvancementTimestamp.parse("2024-05-15T16:30:56Z")
        )
    }

    @Test
    fun `parses an ISO offset date time`() {
        assertEquals(
            Instant.parse("2024-05-15T14:30:56Z"),
            AdvancementTimestamp.parse("2024-05-15T16:30:56+02:00")
        )
    }

    @Test
    fun `returns null for an unparsable value`() {
        assertNull(AdvancementTimestamp.parse("not a timestamp"))
    }

    @Test
    fun `returns null for an empty value`() {
        assertNull(AdvancementTimestamp.parse(""))
    }
}
