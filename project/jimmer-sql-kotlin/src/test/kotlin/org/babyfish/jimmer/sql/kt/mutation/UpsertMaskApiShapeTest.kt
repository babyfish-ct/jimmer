package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import java.math.BigDecimal
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertNotNull

class UpsertMaskApiShapeTest : AbstractMutationTest() {

    @Test
    fun testForbidUpdateAndEmptyUpsertMask() {
        val forbidUpdateCommand: () -> Any = {
            sqlClient.entities.save(
                Book {
                    id = 1L
                    name = "GraphQL in Action"
                    edition = 4
                    price = BigDecimal("80")
                }
            ) {
                forbidUpdate()
            }
        }
        val emptyProps: Array<KProperty1<Book, *>> = emptyArray()
        val emptyMaskCommand: () -> Any = {
            sqlClient.entities.save(
                Book {
                    id = 1L
                    name = "GraphQL in Action"
                    edition = 4
                    price = BigDecimal("80")
                }
            ) {
                setUpsertMask(*emptyProps)
            }
        }
        assertNotNull(forbidUpdateCommand)
        assertNotNull(emptyMaskCommand)
    }
}
