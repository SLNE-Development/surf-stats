package dev.slne.surf.stats.microservice.db.tables

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table

object StatKeysTable : Table("stat_keys") {
    val name = varchar("name", 128).transform({ key(it) }, { it.asString() })

    override val primaryKey = PrimaryKey(name)
}
