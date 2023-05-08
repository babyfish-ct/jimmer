package org.babyfish.jimmer.sql.kt.json

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.tuple
import org.babyfish.jimmer.sql.kt.model.pg.*
import kotlin.test.Test
import kotlin.test.expect

class ScalarProviderTest : AbstractJsonTest() {

    @Test
    fun test() {

        sqlClient.entities.save(
            new(JsonWrapper::class).by {
                id = 1
                point = Point(3, 4)
                tags = listOf("java", "kotlin")
                scores = mapOf(1L to 100)
                complexList = listOf(
                    listOf("1-1", "1-2"),
                    listOf("2-1", "2-2")
                )
                complexMap = mapOf(
                    "key" to mapOf("nested-key" to "value")
                )
            }
        )

        expect(
            "{" +
                "\"id\":1," +
                "\"point\":{\"_x\":3,\"_y\":4}," +
                "\"tags\":[\"java\",\"kotlin\"]," +
                "\"scores\":{\"1\":100}," +
                "\"complexList\":[[\"1-1\",\"1-2\"],[\"2-1\",\"2-2\"]]," +
                "\"complexMap\":{\"key\":{\"nested-key\":\"value\"}}" +
                "}"
        ) {
            sqlClient.entities.findById(JsonWrapper::class, 1L).toString()
        }

        sqlClient.entities.save(
            new(JsonWrapper::class).by {
                id = 1
                point = Point(4, 3)
                tags = listOf("kotlin", "java")
            }
        )
        expect(
            "{" +
                "\"id\":1," +
                "\"point\":{\"_x\":4,\"_y\":3}," +
                "\"tags\":[\"kotlin\",\"java\"]," +
                "\"scores\":{\"1\":100}," +
                "\"complexList\":[[\"1-1\",\"1-2\"],[\"2-1\",\"2-2\"]]," +
                "\"complexMap\":{\"key\":{\"nested-key\":\"value\"}}" +
                "}") {
            sqlClient.entities.findById(JsonWrapper::class, 1L).toString()
        }

        sqlClient
            .createUpdate(JsonWrapper::class) {
                set(table.tags, listOf("java", "kotlin", "scala"))
                where(table.tags eq listOf("kotlin", "java"))
            }
            .execute()
        expect(
            "{" +
                "\"id\":1," +
                "\"point\":{\"_x\":4,\"_y\":3}," +
                "\"tags\":[\"java\",\"kotlin\",\"scala\"]," +
                "\"scores\":{\"1\":100}," +
                "\"complexList\":[[\"1-1\",\"1-2\"],[\"2-1\",\"2-2\"]]," +
                "\"complexMap\":{\"key\":{\"nested-key\":\"value\"}}" +
                "}") {
            sqlClient.entities.findById(JsonWrapper::class, 1L).toString()
        }

        sqlClient
            .createUpdate(JsonWrapper::class) {
                set(table.scores, mapOf(2L to 200))
                where(
                    tuple(table.tags, table.scores) eq Tuple2(
                        listOf("java", "kotlin", "scala"),
                        mapOf(1L to 100)
                    )
                )
            }
            .execute()
        expect(
            "{" +
                "\"id\":1," +
                "\"point\":{\"_x\":4,\"_y\":3}," +
                "\"tags\":[\"java\",\"kotlin\",\"scala\"]," +
                "\"scores\":{\"2\":200}," +
                "\"complexList\":[[\"1-1\",\"1-2\"],[\"2-1\",\"2-2\"]]," +
                "\"complexMap\":{\"key\":{\"nested-key\":\"value\"}}" +
                "}") {
            sqlClient.entities.findById(JsonWrapper::class, 1L).toString()
        }
    }

    @Test
    fun testWhere() {

        sqlClient.entities.save(
            new(JsonWrapper::class).by {
                id = 1
                point = Point(3, 4)
                tags = listOf("java", "kotlin")
                scores = mapOf(1L to 100)
                complexList = listOf(
                    listOf("1-1", "1-2"),
                    listOf("2-1", "2-2")
                )
                complexMap = mapOf(
                    "key" to mapOf("nested-key" to "value")
                )
            }
        )

        expect(
            """[
                |{
                |"id":1,"point":{"_x":3,"_y":4},
                |"tags":["java","kotlin"],
                |"scores":{"1":100},
                |"complexList":[["1-1","1-2"],["2-1","2-2"]],
                |"complexMap":{"key":{"nested-key":"value"}}
                |}
                |]""".trimMargin().replace("\r", "").replace("\n", "")
        ) {
            sqlClient.createQuery(JsonWrapper::class) {
                where(table.point eq Point(3, 4))
                where(table.tags eq listOf("java", "kotlin"))
                select(table)
            }.execute().toString()
        }

        expect(
            """[
                |{
                |"id":1,"point":{"_x":3,"_y":4},
                |"tags":["java","kotlin"],
                |"scores":{"1":100},
                |"complexList":[["1-1","1-2"],["2-1","2-2"]],
                |"complexMap":{"key":{"nested-key":"value"}}
                |}
                |]""".trimMargin().replace("\r", "").replace("\n", "")
        ) {
            sqlClient.createQuery(JsonWrapper::class) {
                where(
                    tuple(table.point, table.tags) eq Tuple2(
                        Point(3, 4),
                        listOf("java", "kotlin")
                    )
                )
                select(table)
            }.execute().toString()
        }
    }

    @Test
    fun testDML() {

        sqlClient.entities.save(
            new(JsonWrapper::class).by {
                id = 1
                point = Point(3, 4)
                tags = listOf("java", "kotlin")
                scores = mapOf(1L to 100)
                complexList = listOf(
                    listOf("1-1", "1-2"),
                    listOf("2-1", "2-2")
                )
                complexMap = mapOf(
                    "key" to mapOf("nested-key" to "value")
                )
            }
        )

        val updateCount = sqlClient
            .createUpdate(JsonWrapper::class) {
                where(table.point eq Point(3, 4))
                where(table.tags eq listOf("java", "kotlin"))
                set(
                    table.scores,
                    mapOf(1L to 100, 2L to 200)
                )
            }
            .execute()
        expect(1) { updateCount }

        expect(
            """[
                |{
                |"id":1,"point":{"_x":3,"_y":4},
                |"tags":["java","kotlin"],
                |"scores":{"1":100,"2":200},
                |"complexList":[["1-1","1-2"],["2-1","2-2"]],
                |"complexMap":{"key":{"nested-key":"value"}}
                |}
                |]""".trimMargin().replace("\r", "").replace("\n", "")
        ) {
            sqlClient.createQuery(JsonWrapper::class) {
                where(
                    tuple(table.point, table.tags) eq Tuple2(
                        Point(3, 4),
                        listOf("java", "kotlin")
                    )
                )
                select(table)
            }.execute().toString()
        }
    }
}