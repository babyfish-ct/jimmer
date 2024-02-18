package org.babyfish.jimmer.sql.kt.model.pg

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id


@DatabaseValidationIgnore
@Entity
interface PgArrayModel {
    @Id
    val id: Long

    val intArr: IntArray
    val integerArr: Array<Int>

    @Column(sqlElementType = "text")
    val textArr: Array<String>

    @Column(sqlElementType = "text")
    val textList: List<String>

    @Column(sqlElementType = "varchar")
    val varcharArr: Array<String>

    @Column(sqlElementType = "varchar")
    val varcharList: List<String>
}
