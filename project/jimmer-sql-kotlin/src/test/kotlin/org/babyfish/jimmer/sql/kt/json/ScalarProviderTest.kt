package org.babyfish.jimmer.sql.kt.json

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.model.pg.JsonWrapper
import org.babyfish.jimmer.sql.kt.model.pg.Point
import org.babyfish.jimmer.sql.kt.model.pg.by
import org.babyfish.jimmer.sql.kt.model.pg.tags
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
            }
        )
        expect("{\"id\":1,\"point\":{\"x\":3,\"y\":4},\"tags\":[\"java\",\"kotlin\"]}") {
            sqlClient.entities.findById(JsonWrapper::class, 1L).toString()
        }

        sqlClient.entities.save(
            new(JsonWrapper::class).by {
                id = 1
                point = Point(4, 3)
                tags = listOf("kotlin", "java")
            }
        )
        expect("{\"id\":1,\"point\":{\"x\":4,\"y\":3},\"tags\":[\"kotlin\",\"java\"]}") {
            sqlClient.entities.findById(JsonWrapper::class, 1L).toString()
        }

        sqlClient
            .createUpdate(JsonWrapper::class) {
                set(table.tags, listOf("java", "kotlin", "scala"))
                where(table.tags eq listOf("kotlin", "java"))
            }
            .execute()
        expect("{\"id\":1,\"point\":{\"x\":4,\"y\":3},\"tags\":[\"java\",\"kotlin\",\"scala\"]}") {
            sqlClient.entities.findById(JsonWrapper::class, 1L).toString()
        }
    }
}