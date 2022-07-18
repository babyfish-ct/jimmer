package org.babyfish.jimmer.benchmark.exposed

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.selectAll

object ExposedDataTable : LongIdTable("DATA", "ID") {

    val value1 = integer("VALUE_1")
    val value2 = integer("VALUE_2")
    val value3 = integer("VALUE_3")
    val value4 = integer("VALUE_4")
    val value5 = integer("VALUE_5")
    val value6 = integer("VALUE_6")
    val value7 = integer("VALUE_7")
    val value8 = integer("VALUE_8")
    val value9 = integer("VALUE_9")

    fun list(): List<ExposedData> =
        ExposedDataTable
            .slice(id, value1, value2, value3, value4, value5, value6, value7, value8, value9)
            .selectAll()
            .map {
                ExposedData(
                    it[id].value,
                    it[value1],
                    it[value2],
                    it[value3],
                    it[value4],
                    it[value5],
                    it[value6],
                    it[value7],
                    it[value8],
                    it[value9]
                )
            }
}