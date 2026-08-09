package dev.slne.surf.stats.core.client.json

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import kotlin.io.path.writeText

class AdvancementsFileServiceTest {

    private val player: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private fun writeAdvancements(directory: Path, uuid: UUID, content: String) {
        directory.resolve("$uuid.json").writeText(content)
    }

    @Test
    fun `loads advancements from disk`(@TempDir directory: Path) = runTest {
        AdvancementsFileService.initialize(directory)
        writeAdvancements(
            directory,
            player,
            """{ "minecraft:story/root": { "criteria": {}, "done": true } }"""
        )

        val entries = AdvancementsFileService.loadAdvancements(player).getOrThrow()

        assertEquals(listOf("minecraft:story/root"), entries.map { it.advancement.asString() })
    }

    @Test
    fun `returns an empty success when the file does not exist`(@TempDir directory: Path) = runTest {
        AdvancementsFileService.initialize(directory)

        val result = AdvancementsFileService.loadAdvancements(player)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `returns a failure for unreadable json`(@TempDir directory: Path) = runTest {
        AdvancementsFileService.initialize(directory)
        writeAdvancements(directory, player, "this is not json")

        assertTrue(AdvancementsFileService.loadAdvancements(player).isFailure)
    }

    @Test
    fun `loads several players and keeps failures separate`(@TempDir directory: Path) = runTest {
        val broken = UUID.fromString("00000000-0000-0000-0000-000000000002")
        AdvancementsFileService.initialize(directory)
        writeAdvancements(
            directory,
            player,
            """{ "minecraft:story/root": { "criteria": {}, "done": true } }"""
        )
        writeAdvancements(directory, broken, "this is not json")

        val results = AdvancementsFileService.loadAllAdvancements(setOf(player, broken))

        assertEquals(1, results.getValue(player).getOrThrow().size)
        assertTrue(results.getValue(broken).isFailure)
    }

    @Test
    fun `reports whether a file exists`(@TempDir directory: Path) = runTest {
        AdvancementsFileService.initialize(directory)

        assertFalse(AdvancementsFileService.advancementsExist(player))

        writeAdvancements(directory, player, "{}")

        assertTrue(AdvancementsFileService.advancementsExist(player))
    }
}
