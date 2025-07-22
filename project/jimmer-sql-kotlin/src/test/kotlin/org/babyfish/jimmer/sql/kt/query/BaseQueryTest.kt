package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.rowCount
import org.babyfish.jimmer.sql.kt.ast.expression.sql
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.storeId
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import kotlin.test.Test

class BaseQueryTest : AbstractQueryTest() {

    @Test
    fun testBaseQueryWithFetch() {
        val baseTable = sqlClient.createBaseQuery(BookStore::class) {
            selector()
                .add(table)
                .add(
                    sql(Int::class, "dense_rank() over(order by %e desc)") {
                        expression(
                            subQuery(Book::class) {
                                where(table.storeId eq parentTable.id)
                                select(rowCount())
                            }
                        )
                    }
                )
        }.asBaseTable()
    }
}