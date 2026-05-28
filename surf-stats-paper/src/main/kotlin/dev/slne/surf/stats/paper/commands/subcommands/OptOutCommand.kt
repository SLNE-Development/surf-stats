package dev.slne.surf.stats.paper.commands.subcommands

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.stats.core.client.service.OptOutStatisticService
import dev.slne.surf.stats.paper.menu.CATEGORY_NAME_CUSTOM
import dev.slne.surf.stats.paper.menu.STAT_NAME_PLAYTIME
import dev.slne.surf.stats.paper.menu.statsOptOutView
import dev.slne.surf.stats.paper.permissions.Permissions
import dev.slne.surf.stats.paper.plugin
import kotlinx.coroutines.withContext

fun optOutCommand() = subcommand("optout") {
    withPermission(Permissions.COMMAND_OPT_OUT)

    playerExecutorSuspend { player, _ ->
        val optOuts = OptOutStatisticService.getOptOutput(player.uniqueId)
        val isPlaytimeOptIn = optOuts.none {
            it.categoryName == key(CATEGORY_NAME_CUSTOM) && it.statisticName == key(
                STAT_NAME_PLAYTIME
            )
        }

        withContext(plugin.entityDispatcher(player)) {
            statsOptOutView.open(player, mutableMapOf("playtime-opt-in" to isPlaytimeOptIn))
        }
    }
}