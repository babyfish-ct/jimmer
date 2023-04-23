package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.link.Student
import org.babyfish.jimmer.sql.kt.model.link.fetchBy
import org.babyfish.jimmer.sql.kt.model.link.name
import kotlin.test.Test

class LinkFetcherTest : AbstractQueryTest() {

    @Test
    fun testFetchView() {
        executeAndExpect(
            sqlClient.createQuery(Student::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        courses({
                            filter {
                                orderBy(table.name.desc())
                            }
                        }) {
                            name()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME 
                    |from STUDENT tb_1_""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.STUDENT_ID, tb_1_.ID, tb_1_.COURSE_ID 
                    |from LEARNING_LINK tb_1_ 
                    |inner join COURSE tb_3_ 
                    |--->on tb_1_.COURSE_ID = tb_3_.ID 
                    |where tb_1_.STUDENT_ID in (?, ?) 
                    |order by tb_3_.NAME desc""".trimMargin()
            )
            statement(2).sql(
                """select tb_1_.ID, tb_1_.NAME 
                    |from COURSE tb_1_ 
                    |where tb_1_.ID in (?, ?, ?)""".trimMargin()
            )
            rows(
                "[" +
                    "--->{" +
                    "--->--->\"id\":1," +
                    "--->--->\"name\":\"Oakes\"," +
                    "--->--->\"courses\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":3," +
                    "--->--->--->--->\"name\":\"SQL\"" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":2," +
                    "--->--->--->--->\"name\":\"Kotlin\"" +
                    "--->--->--->}" +
                    "--->--->]" +
                    "--->}," +
                    "--->{" +
                    "--->--->\"id\":2," +
                    "--->--->\"name\":\"Roach\"," +
                    "--->--->\"courses\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":3," +
                    "--->--->--->--->\"name\":\"SQL\"" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":1," +
                    "--->--->--->--->\"name\":\"Java\"" +
                    "--->--->--->}" +
                    "--->--->]" +
                    "--->}" +
                    "]"
            )
        }
    }

    @Test
    fun testFetchViewAndRaw() {
        executeAndExpect(
            sqlClient.createQuery(Student::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        learningLinks {
                            score()
                        }
                        courses {
                            name()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME 
                    |from STUDENT tb_1_""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.STUDENT_ID, tb_1_.ID, tb_1_.SCORE, tb_1_.COURSE_ID 
                    |from LEARNING_LINK tb_1_ 
                    |where tb_1_.STUDENT_ID in (?, ?)""".trimMargin()
            )
            statement(2).sql(
                """select tb_1_.ID, tb_1_.NAME 
                    |from COURSE tb_1_ 
                    |where tb_1_.ID in (?, ?, ?)""".trimMargin()
            )
            rows(
                "[" +
                    "--->{" +
                    "--->--->\"id\":1," +
                    "--->--->\"name\":\"Oakes\"," +
                    "--->--->\"learningLinks\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":1," +
                    "--->--->--->--->\"course\":{" +
                    "--->--->--->--->--->\"id\":2," +
                    "--->--->--->--->--->\"name\":\"Kotlin\"" +
                    "--->--->--->--->}," +
                    "--->--->--->--->\"score\":78" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":2," +
                    "--->--->--->--->\"course\":{" +
                    "--->--->--->--->--->\"id\":3," +
                    "--->--->--->--->--->\"name\":\"SQL\"" +
                    "--->--->--->--->}," +
                    "--->--->--->--->\"score\":null" +
                    "--->--->--->}" +
                    "--->--->]," +
                    "--->--->\"courses\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":2," +
                    "--->--->--->--->\"name\":\"Kotlin\"" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":3," +
                    "--->--->--->--->\"name\":\"SQL\"" +
                    "--->--->--->}" +
                    "--->--->]" +
                    "--->}," +
                    "--->{" +
                    "--->--->\"id\":2," +
                    "--->--->\"name\":\"Roach\"," +
                    "--->--->\"learningLinks\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":3," +
                    "--->--->--->--->\"course\":{" +
                    "--->--->--->--->--->\"id\":3," +
                    "--->--->--->--->--->\"name\":\"SQL\"" +
                    "--->--->--->--->}," +
                    "--->--->--->--->\"score\":87" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":4," +
                    "--->--->--->--->\"course\":{" +
                    "--->--->--->--->--->\"id\":1," +
                    "--->--->--->--->--->\"name\":\"Java\"" +
                    "--->--->--->--->}," +
                    "--->--->--->--->\"score\":null" +
                    "--->--->--->}" +
                    "--->--->]," +
                    "--->--->\"courses\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":3," +
                    "--->--->--->--->\"name\":\"SQL\"" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":1," +
                    "--->--->--->--->\"name\":\"Java\"" +
                    "--->--->--->}" +
                    "--->--->]" +
                    "--->}" +
                    "]"
            )
        }
    }

    @Test
    fun testFetchViewAndRawWithChild() {
        executeAndExpect(
            sqlClient.createQuery(Student::class) {
                select(
                    table.fetchBy {
                        allScalarFields()
                        learningLinks {
                            score()
                            course {
                                name()
                            }
                        }
                        courses {
                            academicCredit()
                        }
                    }
                )
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME 
                    |from STUDENT tb_1_""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.STUDENT_ID, tb_1_.ID, tb_1_.SCORE, tb_1_.COURSE_ID 
                    |from LEARNING_LINK tb_1_ 
                    |where tb_1_.STUDENT_ID in (?, ?)""".trimMargin()
            )
            statement(2).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.ACADEMIC_CREDIT 
                    |from COURSE tb_1_ 
                    |where tb_1_.ID in (?, ?, ?)""".trimMargin()
            )
            rows(
                "[" +
                    "--->{" +
                    "--->--->\"id\":1," +
                    "--->--->\"name\":\"Oakes\"," +
                    "--->--->\"learningLinks\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":1," +
                    "--->--->--->--->\"course\":{" +
                    "--->--->--->--->--->\"id\":2," +
                    "--->--->--->--->--->\"name\":\"Kotlin\"," +
                    "--->--->--->--->--->\"academicCredit\":2" +
                    "--->--->--->--->}," +
                    "--->--->--->--->\"score\":78" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":2," +
                    "--->--->--->--->\"course\":{" +
                    "--->--->--->--->--->\"id\":3," +
                    "--->--->--->--->--->\"name\":\"SQL\"," +
                    "--->--->--->--->--->\"academicCredit\":2" +
                    "--->--->--->--->}," +
                    "--->--->--->--->\"score\":null" +
                    "--->--->--->}" +
                    "--->--->]," +
                    "--->--->\"courses\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":2," +
                    "--->--->--->--->\"name\":\"Kotlin\"," +
                    "--->--->--->--->\"academicCredit\":2" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":3," +
                    "--->--->--->--->\"name\":\"SQL\"," +
                    "--->--->--->--->\"academicCredit\":2" +
                    "--->--->--->}" +
                    "--->--->]" +
                    "--->}," +
                    "--->{" +
                    "--->--->\"id\":2," +
                    "--->--->\"name\":\"Roach\"," +
                    "--->--->\"learningLinks\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":3," +
                    "--->--->--->--->\"course\":{" +
                    "--->--->--->--->--->\"id\":3," +
                    "--->--->--->--->--->\"name\":\"SQL\"," +
                    "--->--->--->--->--->\"academicCredit\":2" +
                    "--->--->--->--->}," +
                    "--->--->--->--->\"score\":87" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":4," +
                    "--->--->--->--->\"course\":{" +
                    "--->--->--->--->--->\"id\":1," +
                    "--->--->--->--->--->\"name\":\"Java\"," +
                    "--->--->--->--->--->\"academicCredit\":2" +
                    "--->--->--->--->}," +
                    "--->--->--->--->\"score\":null" +
                    "--->--->--->}" +
                    "--->--->]," +
                    "--->--->\"courses\":[" +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":3," +
                    "--->--->--->--->\"name\":\"SQL\"," +
                    "--->--->--->--->\"academicCredit\":2" +
                    "--->--->--->}," +
                    "--->--->--->{" +
                    "--->--->--->--->\"id\":1," +
                    "--->--->--->--->\"name\":\"Java\"," +
                    "--->--->--->--->\"academicCredit\":2" +
                    "--->--->--->}" +
                    "--->--->]" +
                    "--->}" +
                    "]"
            )
        }
    }
}