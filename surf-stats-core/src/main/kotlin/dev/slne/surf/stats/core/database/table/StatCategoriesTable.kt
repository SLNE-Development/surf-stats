package dev.slne.surf.stats.core.database.table

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table

object StatCategoriesTable : Table("stat_categories") {
    val name = varchar("name", 128)

    override val primaryKey = PrimaryKey(name)
}
