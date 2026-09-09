package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.ast.mutation.KBatchSaveResult
import org.babyfish.jimmer.sql.kt.ast.mutation.KSaveCommandPartialDsl
import org.babyfish.jimmer.sql.kt.ast.mutation.KSimpleSaveResult
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchByKeyDslTest : AbstractMutationTest() {

    @Test
    fun testSingleAndBatchCommandReceivers() {
        val client = sqlClient { setDialect(H2Dialect()) }
        val input = Book {
            id = 900001L
            name = "matchByKey DSL"
            edition = 1
            price = BigDecimal.ONE
        }
        val configure: KSaveCommandPartialDsl.() -> Unit = {
            matchByKey()
            forbidUpdate()
        }
        jdbc { con ->
            val single: KSimpleSaveResult<Book> = client.saveCommand(input, SaveMode.UPSERT, block = configure).execute(con)
            val batch: KBatchSaveResult<Book> = client.saveEntitiesCommand(listOf(Book(input) { id = 900002L })) {
                matchByKey()
                forbidUpdate()
            }.execute(con)
            assertTrue(single.isAccepted)
            assertTrue(batch.items.single().isAccepted)
            assertEquals(input.id, batch.items.single().modifiedEntity.id)
        }
    }
}
