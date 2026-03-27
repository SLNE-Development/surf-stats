package dev.slne.surf.stats.paper.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.stats.paper.menu.openStatsSettingsMenu
import dev.slne.surf.stats.paper.permissions.Permissions

fun optOutCommand() = subcommand("optout") {
    withPermission(Permissions.COMMAND_OPT_OUT)

    playerExecutor { player, _ ->
        openStatsSettingsMenu(player)
    }
}