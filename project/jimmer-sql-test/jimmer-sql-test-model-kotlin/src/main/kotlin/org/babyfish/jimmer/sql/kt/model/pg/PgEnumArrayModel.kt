package org.babyfish.jimmer.sql.kt.model.pg

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.kt.model.classic.author.Gender

@DatabaseValidationIgnore
@Entity
interface PgEnumArrayModel {
    @Id
    val id: Long

    @Column(sqlElementType = "varchar")
    val genderArr: Array<Gender>

    val textList: List<Gender>

    @Column(sqlElementType = "varchar")
    val varcharList: List<Gender>

    val priorities: List<Priority>
}
