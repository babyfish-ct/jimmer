package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.isNotNull
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.link.*
import kotlin.test.Test

class LinkDSLTest : AbstractQueryTest() {

    @Test
    fun testFetcher() {
        executeAndExpect(
            sqlClient.createQuery(Student::class) {
                where(table.asTableEx().courses.name eq "SQL")
                where(table.asTableEx().learningLinks.score.isNotNull())
                select(
                    table.asTableEx().courses.fetchBy {
                        allScalarFields()
                        students {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                "select tb_3_.ID, tb_3_.NAME, tb_3_.ACADEMIC_CREDIT " +
                    "from STUDENT tb_1_ " +
                    "inner join LEARNING_LINK tb_2_ " +
                    "--->on tb_1_.ID = tb_2_.STUDENT_ID " +
                    "inner join COURSE tb_3_ " +
                    "--->on tb_2_.COURSE_ID = tb_3_.ID " +
                    "where " +
                    "--->tb_3_.NAME = ? " +
                    "and " +
                    "--->tb_2_.SCORE is not null"
            )
            statement(1).sql(
                "select tb_1_.ID, tb_1_.STUDENT_ID " +
                    "from LEARNING_LINK tb_1_ " +
                    "where tb_1_.COURSE_ID = ?"
            )
            statement(2).sql(
                ("select tb_1_.ID, tb_1_.NAME " +
                    "from STUDENT tb_1_ " +
                    "where tb_1_.ID in (?, ?)")
            )
            rows(
                ("[" +
                    "--->{" +
                    "--->--->\"id\":3," +
                    "--->--->\"name\":\"SQL\"," +
                    "--->--->\"academicCredit\":2," +
                    "--->--->\"students\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":1," +
                    "--->--->--->--->\"name\":\"Oakes\"" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":2," +
                    "--->--->--->--->\"name\":\"Roach\"" +
                    "--->--->--->}" +
                    "--->--->]" +
                    "--->}" +
                    "]")
            )
        }
    }

    @Test
    fun testImplicitSubQueryForIssue776() {
        executeAndExpect(
            sqlClient.createQuery(Student::class) {
                where += table.courses {
                    id eq 1L
                }
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME 
                    |from STUDENT tb_1_ 
                    |where exists(
                    |--->select 1 
                    |--->from COURSE tb_2_ 
                    |--->inner join LEARNING_LINK tb_3_ 
                    |--->--->on tb_2_.ID = tb_3_.COURSE_ID 
                    |--->where tb_3_.STUDENT_ID = tb_1_.ID 
                    |--->and tb_2_.ID = ?
                    |)""".trimMargin()
            )
            rows("[{\"id\":2,\"name\":\"Roach\"}]")
        }
    }
}