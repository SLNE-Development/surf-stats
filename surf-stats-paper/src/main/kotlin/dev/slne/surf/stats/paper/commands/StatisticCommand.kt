package dev.slne.surf.stats.paper.commands

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.stats.paper.commands.subcommands.optOutCommand
import dev.slne.surf.stats.paper.permissions.Permissions

fun statsCommand() = commandAPICommand("stats") {
    withPermission(Permissions.COMMAND_GENERIC)

    withSubcommand(optOutCommand())
}