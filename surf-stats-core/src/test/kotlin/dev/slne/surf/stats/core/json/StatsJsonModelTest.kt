package dev.slne.surf.stats.core.json

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StatsJsonModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `should parse empty stats`() {
        val jsonString = """{"stats": {}}"""

        val model = json.decodeFromString<StatsJsonModel>(jsonString)

        assertTrue(model.stats.isEmpty())
        assertEquals(0, model.dataVersion)
    }

    @Test
    fun `should parse stats with DataVersion`() {
        val jsonString = """
            {
                "stats": {},
                "DataVersion": 3953
            }
        """.trimIndent()

        val model = json.decodeFromString<StatsJsonModel>(jsonString)

        assertEquals(3953, model.dataVersion)
    }

    @Test
    fun `should parse minecraft stats format`() {
        val jsonString = """
            {
                "stats": {
                    "minecraft:mined": {
                        "minecraft:stone": 100,
                        "minecraft:dirt": 50
                    },
                    "minecraft:killed": {
                        "minecraft:zombie": 25
                    }
                },
                "DataVersion": 3953
            }
        """.trimIndent()

        val model = json.decodeFromString<StatsJsonModel>(jsonString)

        assertEquals(2, model.stats.size)
        assertEquals(100L, model.stats["minecraft:mined"]?.get("minecraft:stone"))
        assertEquals(50L, model.stats["minecraft:mined"]?.get("minecraft:dirt"))
        assertEquals(25L, model.stats["minecraft:killed"]?.get("minecraft:zombie"))
    }

    @Test
    fun `should convert to stat entries`() {
        val jsonString = """
            {
                "stats": {
                    "minecraft:mined": {
                        "minecraft:stone": 100
                    },
                    "minecraft:killed": {
                        "minecraft:zombie": 25
                    }
                }
            }
        """.trimIndent()

        val model = json.decodeFromString<StatsJsonModel>(jsonString)
        val entries = model.toStatEntries()

        assertEquals(2, entries.size)

        val stoneEntry = entries.find { it.category == "minecraft:mined" && it.key == "minecraft:stone" }
        assertNotNull(stoneEntry)
        assertEquals(100L, stoneEntry?.value)

        val zombieEntry = entries.find { it.category == "minecraft:killed" && it.key == "minecraft:zombie" }
        assertNotNull(zombieEntry)
        assertEquals(25L, zombieEntry?.value)
    }

    @Test
    fun `should get category stats`() {
        val jsonString = """
            {
                "stats": {
                    "minecraft:mined": {
                        "minecraft:stone": 100,
                        "minecraft:dirt": 50
                    }
                }
            }
        """.trimIndent()

        val model = json.decodeFromString<StatsJsonModel>(jsonString)

        val minedStats = model.getCategory("minecraft:mined")
        assertEquals(2, minedStats.size)
        assertEquals(100L, minedStats["minecraft:stone"])

        val emptyCategory = model.getCategory("minecraft:unknown")
        assertTrue(emptyCategory.isEmpty())
    }

    @Test
    fun `should extract category names`() {
        val jsonString = """
            {
                "stats": {
                    "minecraft:mined": {"minecraft:stone": 100},
                    "minecraft:killed": {"minecraft:zombie": 25},
                    "minecraft:custom": {"minecraft:play_time": 72000}
                }
            }
        """.trimIndent()

        val model = json.decodeFromString<StatsJsonModel>(jsonString)
        val categories = model.categoryNames()

        assertEquals(3, categories.size)
        assertTrue(categories.contains("minecraft:mined"))
        assertTrue(categories.contains("minecraft:killed"))
        assertTrue(categories.contains("minecraft:custom"))
    }

    @Test
    fun `should extract stat key names`() {
        val jsonString = """
            {
                "stats": {
                    "minecraft:mined": {"minecraft:stone": 100, "minecraft:dirt": 50},
                    "minecraft:killed": {"minecraft:zombie": 25}
                }
            }
        """.trimIndent()

        val model = json.decodeFromString<StatsJsonModel>(jsonString)
        val keys = model.statKeyNames()

        assertEquals(3, keys.size)
        assertTrue(keys.contains("minecraft:stone"))
        assertTrue(keys.contains("minecraft:dirt"))
        assertTrue(keys.contains("minecraft:zombie"))
    }

    @Test
    fun `should handle unknown fields gracefully`() {
        val jsonString = """
            {
                "stats": {},
                "DataVersion": 3953,
                "unknownField": "should be ignored",
                "anotherUnknown": 123
            }
        """.trimIndent()

        val model = json.decodeFromString<StatsJsonModel>(jsonString)

        assertNotNull(model)
        assertEquals(3953, model.dataVersion)
    }
}
