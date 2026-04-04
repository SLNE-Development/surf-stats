package dev.slne.surf.stats.paper.permissions

import dev.slne.surf.api.paper.permission.PermissionRegistry

object Permissions : PermissionRegistry() {
    const val BASE = "surf.stats"
    const val BASE_COMMAND = "$BASE.command"

    val COMMAND_GENERIC = create("$BASE_COMMAND.generic")
    val COMMAND_OPT_OUT = create("$BASE_COMMAND.opt-out")
}