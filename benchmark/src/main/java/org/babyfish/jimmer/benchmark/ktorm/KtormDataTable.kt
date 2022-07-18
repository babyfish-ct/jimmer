package org.babyfish.jimmer.benchmark.ktorm

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long

object KtormDataTable : Table<KtormData>("DATA") {

    val id = long("ID").primaryKey().bindTo { it.id }
    val value1 = int("VALUE_1").bindTo { it.value1 }
    val value2 = int("VALUE_2").bindTo { it.value2 }
    val value3 = int("VALUE_3").bindTo { it.value3 }
    val value4 = int("VALUE_4").bindTo { it.value4 }
    val value5 = int("VALUE_5").bindTo { it.value5 }
    val value6 = int("VALUE_6").bindTo { it.value6 }
    val value7 = int("VALUE_7").bindTo { it.value7 }
    val value8 = int("VALUE_8").bindTo { it.value8 }
    val value9 = int("VALUE_9").bindTo { it.value9 }
}