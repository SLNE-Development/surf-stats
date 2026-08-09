package dev.slne.surf.stats.core.client.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AdvancementsJsonModelTest {

    @Test
    fun `ignores the DataVersion key`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "minecraft:story/root": { "criteria": { "crafting_table": "2024-05-15 16:04:02 +0000" }, "done": true },
              "DataVersion": 4189
            }
            """.trimIndent()
        )

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `filters recipe advancements in any namespace`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "minecraft:recipes/misc/charcoal": { "criteria": { "has_log": "2024-05-15 16:04:02 +0000" }, "done": true },
              "mypack:recipes/custom/thing": { "criteria": {}, "done": false },
              "minecraft:story/root": { "criteria": {}, "done": true }
            }
            """.trimIndent()
        )

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `keeps non-recipe advancements from foreign namespaces`() {
        val entries = AdvancementsJsonModel.parse(
            """
            { "mypack:custom/thing": { "criteria": {}, "done": true } }
            """.trimIndent()
        )

        assertEquals(listOf("mypack:custom/thing"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `skips malformed advancement identifiers`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "Not A Valid Key": { "criteria": {}, "done": true },
              "minecraft:story/root": { "criteria": {}, "done": true }
            }
            """.trimIndent()
        )

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `skips entries whose body is not an advancement object`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "minecraft:story/broken": "unexpected",
              "minecraft:story/root": { "criteria": {}, "done": true }
            }
            """.trimIndent()
        )

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `maps criteria with their timestamps`() {
        val entries = AdvancementsJsonModel.parse(
            """
            {
              "minecraft:adventure/adventuring_time": {
                "criteria": {
                  "minecraft:plains": "2024-05-15 16:30:56 +0000",
                  "minecraft:desert": "2024-05-16 09:02:11 +0000"
                },
                "done": false
              }
            }
            """.trimIndent()
        )

        val entry = entries.single()
        assertFalse(entry.done)
        assertEquals(2, entry.criteriaDone)
        assertNull(entry.completedAt)
        assertEquals(
            Instant.parse("2024-05-15T16:30:56Z"),
            entry.criteria.single { it.name == "minecraft:plains" }.awardedAt
        )
    }

    @Test
    fun `keeps criteria whose timestamp cannot be parsed`() {
        val entries = AdvancementsJsonModel.parse(
            """
            { "minecraft:story/root": { "criteria": { "crafting_table": "garbage" }, "done": true } }
            """.trimIndent()
        )

        val criterion = entries.single().criteria.single()
        assertEquals("crafting_table", criterion.name)
        assertNull(criterion.awardedAt)
    }

    @Test
    fun `returns an empty list for an empty object`() {
        assertTrue(AdvancementsJsonModel.parse("{}").isEmpty())
    }

    @Test
    fun `returns an empty list when only DataVersion is present`() {
        assertTrue(AdvancementsJsonModel.parse("""{ "DataVersion": 4189 }""").isEmpty())
    }
}
