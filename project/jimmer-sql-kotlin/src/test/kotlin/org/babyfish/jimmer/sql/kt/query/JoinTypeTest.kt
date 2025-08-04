package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.`ilike?`
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.store
import org.babyfish.jimmer.sql.kt.model.classic.book.`store?`
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.junit.Test

class JoinTypeTest : AbstractQueryTest() {

    @Test
    fun testOuterJoin() {
        executeAndExpect(
            createQuery(null)
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |order by tb_2_.NAME asc""".trimMargin()
            )
        }
    }

    @Test
    fun testInnerJoin() {
        executeAndExpect(
            createQuery("MANNING")
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID 
                    |where lower(tb_2_.NAME) like ? 
                    |order by tb_2_.NAME asc""".trimMargin()
            )
        }
    }

    private fun createQuery(storeName: String?): KConfigurableRootQuery<KNonNullTable<Book>, Book> =
        sqlClient
            .createQuery(Book::class) {
                where(table.store.name `ilike?` storeName)
                orderBy(table.`store?`.name.asc())
                select(table)
            }
}