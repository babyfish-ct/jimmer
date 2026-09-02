package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.gt
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandPartialDsl
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertNotNull

class SaveUpdateWhereApiShapeTest : AbstractMutationTest() {

    @Test
    fun testReceiverAndExpressionTyping() {
        val condition: KSaveCommandPartialDsl.UpdateConditionContext<Book>.() -> KNonNullExpression<Boolean>? = {
            newNonNull(Book::edition) gt table.edition
        }
        val command: () -> Any = {
            sqlClient.entities.save(
                Book {
                    id = 1L
                    name = "GraphQL in Action"
                    edition = 4
                    price = BigDecimal("80")
                }
            ) {
                setMode(SaveMode.UPSERT)
                setUpdateWhere(Book::class, condition)
                setOptimisticLock(Book::class, block = condition)
            }
        }
        assertNotNull(command)
    }
}
