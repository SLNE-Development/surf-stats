package dev.slne.surf.stats.core.client.service

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.stats.api.model.AdvancementCriterion
import dev.slne.surf.stats.api.model.AdvancementEntry
import dev.slne.surf.stats.api.model.PlayerAdvancements
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class AdvancementSyncStateServiceTest {

    private val player: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val other: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private val awardedAt = Instant.parse("2024-05-15T16:00:00Z")

    @BeforeEach
    fun resetState() {
        AdvancementSyncStateService.forget(player)
        AdvancementSyncStateService.forget(other)
    }

    private fun snapshot(
        uuid: UUID = player,
        serverName: String = "survival",
        advancements: List<AdvancementEntry>
    ) = PlayerAdvancements(uuid, serverName, advancements)

    private fun root(done: Boolean = true, criteria: List<AdvancementCriterion> = emptyList()) =
        AdvancementEntry(key("minecraft:story/root"), done, criteria)

    private fun mineDiamond() = AdvancementEntry(key("minecraft:story/mine_diamond"), true)

    @Test
    fun `selects every snapshot on the first sync`() {
        val snapshots = listOf(snapshot(advancements = listOf(root())))

        assertEquals(snapshots, AdvancementSyncStateService.selectChanged(snapshots))
    }

    @Test
    fun `skips an unchanged snapshot after it was marked synced`() {
        val snapshots = listOf(snapshot(advancements = listOf(root())))
        AdvancementSyncStateService.markSynced(snapshots, failed = emptySet())

        assertTrue(AdvancementSyncStateService.selectChanged(snapshots).isEmpty())
    }

    @Test
    fun `ignores ordering differences`() {
        val ordered = listOf(snapshot(advancements = listOf(root(), mineDiamond())))
        AdvancementSyncStateService.markSynced(ordered, failed = emptySet())

        val reordered = listOf(snapshot(advancements = listOf(mineDiamond(), root())))

        assertTrue(AdvancementSyncStateService.selectChanged(reordered).isEmpty())
    }

    @Test
    fun `ignores criterion ordering differences`() {
        val ordered = listOf(
            snapshot(
                advancements = listOf(
                    root(
                        done = false,
                        criteria = listOf(
                            AdvancementCriterion("a", awardedAt),
                            AdvancementCriterion("b", awardedAt),
                        )
                    )
                )
            )
        )
        AdvancementSyncStateService.markSynced(ordered, failed = emptySet())

        val reordered = listOf(
            snapshot(
                advancements = listOf(
                    root(
                        done = false,
                        criteria = listOf(
                            AdvancementCriterion("b", awardedAt),
                            AdvancementCriterion("a", awardedAt),
                        )
                    )
                )
            )
        )

        assertTrue(AdvancementSyncStateService.selectChanged(reordered).isEmpty())
    }

    @Test
    fun `detects an added advancement`() {
        AdvancementSyncStateService.markSynced(
            listOf(snapshot(advancements = listOf(root()))),
            failed = emptySet()
        )

        val grown = listOf(snapshot(advancements = listOf(root(), mineDiamond())))

        assertEquals(grown, AdvancementSyncStateService.selectChanged(grown))
    }

    @Test
    fun `detects an added criterion`() {
        AdvancementSyncStateService.markSynced(
            listOf(
                snapshot(
                    advancements = listOf(
                        root(done = false, criteria = listOf(AdvancementCriterion("a", awardedAt)))
                    )
                )
            ),
            failed = emptySet()
        )

        val grown = listOf(
            snapshot(
                advancements = listOf(
                    root(
                        done = false,
                        criteria = listOf(
                            AdvancementCriterion("a", awardedAt),
                            AdvancementCriterion("b", awardedAt),
                        )
                    )
                )
            )
        )

        assertEquals(grown, AdvancementSyncStateService.selectChanged(grown))
    }

    @Test
    fun `detects a removed advancement`() {
        AdvancementSyncStateService.markSynced(
            listOf(snapshot(advancements = listOf(root(), mineDiamond()))),
            failed = emptySet()
        )

        val shrunk = listOf(snapshot(advancements = listOf(root())))

        assertEquals(shrunk, AdvancementSyncStateService.selectChanged(shrunk))
    }

    @Test
    fun `detects a changed done flag`() {
        AdvancementSyncStateService.markSynced(
            listOf(snapshot(advancements = listOf(root(done = false)))),
            failed = emptySet()
        )

        val completed = listOf(snapshot(advancements = listOf(root(done = true))))

        assertEquals(completed, AdvancementSyncStateService.selectChanged(completed))
    }

    @Test
    fun `does not mark failed players as synced`() {
        val snapshots = listOf(
            snapshot(uuid = player, advancements = listOf(root())),
            snapshot(uuid = other, advancements = listOf(root())),
        )
        AdvancementSyncStateService.markSynced(snapshots, failed = setOf(player))

        assertEquals(
            listOf(player),
            AdvancementSyncStateService.selectChanged(snapshots).map { it.playerUuid }
        )
    }

    @Test
    fun `forget makes the next snapshot count as changed`() {
        val snapshots = listOf(snapshot(advancements = listOf(root())))
        AdvancementSyncStateService.markSynced(snapshots, failed = emptySet())

        AdvancementSyncStateService.forget(player)

        assertEquals(snapshots, AdvancementSyncStateService.selectChanged(snapshots))
    }

    @Test
    fun `detects a changed server name`() {
        AdvancementSyncStateService.markSynced(
            listOf(snapshot(serverName = "survival", advancements = listOf(root()))),
            failed = emptySet()
        )

        val otherServer = listOf(snapshot(serverName = "creative", advancements = listOf(root())))

        assertEquals(otherServer, AdvancementSyncStateService.selectChanged(otherServer))
    }
}
