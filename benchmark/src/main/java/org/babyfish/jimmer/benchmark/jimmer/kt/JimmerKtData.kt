package org.babyfish.jimmer.benchmark.jimmer.kt

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "DATA")
interface JimmerKtData {

    @Id
    val id: Long

    @Column(name = "VALUE_1")
    val value1: Int

    @Column(name = "VALUE_2")
    val value2: Int

    @Column(name = "VALUE_3")
    val value3: Int

    @Column(name = "VALUE_4")
    val value4: Int

    @Column(name = "VALUE_5")
    val value5: Int

    @Column(name = "VALUE_6")
    val value6: Int

    @Column(name = "VALUE_7")
    val value7: Int

    @Column(name = "VALUE_8")
    val value8: Int

    @Column(name = "VALUE_9")
    val value9: Int
}