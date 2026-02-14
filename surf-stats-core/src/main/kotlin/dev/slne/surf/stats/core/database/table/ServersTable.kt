package dev.slne.surf.stats.core.database.table

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.timestamp

object ServersTable : Table("servers") {
    val name = varchar("name", 128)
    val label = text("label")
    val active = bool("active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(name)
}
