package dev.slne.surf.stats.core.database.table

import dev.slne.surf.database.columns.charUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.timestamp

object PlayersTable : Table("players") {
    val uuid = charUuid("uuid")
    val name = varchar("name", 36)
    val dataVersion = integer("dataversion")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(uuid)
}
