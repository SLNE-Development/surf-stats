package dev.slne.surf.stats.core.client.model

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.stats.api.model.AdvancementCriterion
import dev.slne.surf.stats.api.model.AdvancementEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class AdvancementEntryTest {

    private val root = key("minecraft:story/root")

    @Test
    fun `completedAt is the latest criterion timestamp when done`() {
        val entry = AdvancementEntry(
            advancement = root,
            done = true,
            criteria = listOf(
                AdvancementCriterion("a", Instant.parse("2024-05-15T16:00:00Z")),
                AdvancementCriterion("b", Instant.parse("2024-05-16T16:00:00Z")),
            )
        )

        assertEquals(Instant.parse("2024-05-16T16:00:00Z"), entry.completedAt)
    }

    @Test
    fun `completedAt is null when the advancement is not done`() {
        val entry = AdvancementEntry(
            advancement = root,
            done = false,
            criteria = listOf(AdvancementCriterion("a", Instant.parse("2024-05-15T16:00:00Z")))
        )

        assertNull(entry.completedAt)
    }

    @Test
    fun `completedAt is null when done but no timestamp parsed`() {
        val entry = AdvancementEntry(
            advancement = root,
            done = true,
            criteria = listOf(AdvancementCriterion("a", null))
        )

        assertNull(entry.completedAt)
    }

    @Test
    fun `criteriaDone counts every awarded criterion including undated ones`() {
        val entry = AdvancementEntry(
            advancement = root,
            done = false,
            criteria = listOf(
                AdvancementCriterion("a", Instant.parse("2024-05-15T16:00:00Z")),
                AdvancementCriterion("b", null),
            )
        )

        assertEquals(2, entry.criteriaDone)
    }

    @Test
    fun `criteriaDone is zero without criteria`() {
        assertEquals(0, AdvancementEntry(advancement = root, done = false).criteriaDone)
    }
}
