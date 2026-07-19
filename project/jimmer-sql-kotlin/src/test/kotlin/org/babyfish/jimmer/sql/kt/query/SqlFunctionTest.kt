package org.babyfish.jimmer.sql.kt.query

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.babyfish.jimmer.jackson.v2.JsonCodecV2
import org.babyfish.jimmer.sql.ast.SqlTimeUnit
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.ast.expression.diff
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.minus
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.Administrator
import org.babyfish.jimmer.sql.kt.model.inheritance.createdTime
import org.babyfish.jimmer.sql.kt.model.inheritance.id
import org.babyfish.jimmer.sql.kt.model.inheritance.modifiedTime
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test

class SqlFunctionTest : AbstractQueryTest() {

    @Test
    fun testMinus() {
        executeAndExpect(
            sqlClient { setDialect(H2Dialect()) }
                .createQuery(Administrator::class) {
                    where(table.id eq 1L)
                    select(
                        table.modifiedTime,
                        table.modifiedTime.minus(2, SqlTimeUnit.MONTHS)
                    )
                }
        ) {
            sql(
                """select tb_1_.MODIFIED_TIME, dateadd(month, ?, tb_1_.MODIFIED_TIME) 
                    |from ADMINISTRATOR tb_1_ 
                    |where tb_1_.ID = ? and tb_1_.DELETED <> ?""".trimMargin()
            )
            rows(
                """[{"_1":"2022-10-03T00:10:00","_2":"2022-08-03T00:10:00"}]"""
            )
        }
    }

    @Test
    fun testDiff() {
        executeAndExpect(
            sqlClient { setDialect(H2Dialect()) }
                .createQuery(Administrator::class) {
                    where(table.id eq 1L)
                    select(
                        table.modifiedTime.diff(
                            table.createdTime,
                            SqlTimeUnit.MINUTES
                        )
                    )
                }
        ) {
            sql(
                """select (tb_1_.MODIFIED_TIME - tb_1_.CREATED_TIME) * 1440 
                    |from ADMINISTRATOR tb_1_ 
                    |where tb_1_.ID = ? and tb_1_.DELETED <> ?""".trimMargin()
            )
            rows(
                """[10.0]"""
            )
        }
    }
}