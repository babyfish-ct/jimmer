package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.Personal
import org.babyfish.jimmer.sql.kt.model.dto.PersonalPhoneInput
import org.babyfish.jimmer.sql.kt.model.dto.PersonalPhoneView
import org.babyfish.jimmer.sql.kt.model.id
import org.junit.Test

class JsonConverterTest : AbstractQueryTest() {
    @Test
    fun testInput() {
        // Converter does not support the `input` method
        PersonalPhoneInput(phone = "12345678910").toEntity()
    }

    @Test
    fun testView() {
        executeAndExpect(
            sqlClient.createQuery(Personal::class) {
                where(table.id eq 1)
                select(table.fetch(PersonalPhoneView::class))
            }
        ) {
            statement(0).sql(
                """select tb_1_.ID, tb_1_.PHONE from PERSONAL tb_1_ where tb_1_.ID = ?""".trimMargin()
            )
            rows("""[{"phone":"123****8910"}]""")
        }
    }
}