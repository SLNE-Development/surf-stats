package dev.slne.surf.stats.microservice.db.tables

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.database.columns.charUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table

object PlayerStatOptOuts : Table("player_stat_optouts") {
    val playerUuid = charUuid("player_uuid")
    val categoryName = varchar("category_name", 128)
        .transform({ key(it) }, { it.asString() })
        .references(StatCategoriesTable.name)
    val statKeyName = varchar("stat_key_name", 128)
        .transform({ key(it) }, { it.asString() })
        .references(StatKeysTable.name)
}