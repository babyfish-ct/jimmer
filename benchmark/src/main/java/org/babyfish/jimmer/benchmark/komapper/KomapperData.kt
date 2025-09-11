package org.babyfish.jimmer.benchmark.komapper

import org.komapper.annotation.KomapperColumn
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable

@KomapperEntity
@KomapperTable(name = "DATA")
data class KomapperData(
    @KomapperId
    @KomapperColumn(name = "ID")
    val id: Long,
    @KomapperColumn(name = "VALUE_1")
    val value1: Int,
    @KomapperColumn(name = "VALUE_2")
    val value2: Int,
    @KomapperColumn(name = "VALUE_3")
    val value3: Int,
    @KomapperColumn(name = "VALUE_4")
    val value4: Int,
    @KomapperColumn(name = "VALUE_5")
    val value5: Int,
    @KomapperColumn(name = "VALUE_6")
    val value6: Int,
    @KomapperColumn(name = "VALUE_7")
    val value7: Int,
    @KomapperColumn(name = "VALUE_8")
    val value8: Int,
    @KomapperColumn(name = "VALUE_9")
    val value9: Int
)
