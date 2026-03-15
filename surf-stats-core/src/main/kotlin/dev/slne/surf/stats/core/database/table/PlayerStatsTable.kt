package dev.slne.surf.stats.core.database.table

import dev.slne.surf.database.columns.charUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table

object PlayerStatsTable : Table("player_stats") {
    val playerUuid = charUuid("player_uuid").references(PlayersTable.uuid)
    val categoryName = varchar("category_name", 128).references(StatCategoriesTable.name)
    val statKeyName = varchar("stat_key_name", 128).references(StatKeysTable.name)
    val value = long("value")
    val serverName = varchar("server_name", 128).references(ServersTable.name)

    override val primaryKey = PrimaryKey(playerUuid, categoryName, statKeyName, serverName)
}
