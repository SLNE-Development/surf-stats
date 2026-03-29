package dev.slne.surf.stats.paper.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.stats.paper.menu.StatsOptOutView
import dev.slne.surf.stats.paper.permissions.Permissions
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.viewFrame

fun optOutCommand() = subcommand("optout") {
    withPermission(Permissions.COMMAND_OPT_OUT)

    playerExecutor { player, _ ->
        viewFrame.open(StatsOptOutView::class.java, player)
    }
}