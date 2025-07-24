package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.storeId
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.fetchBy
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.classic.store.name
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
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 le 2)
                where(table._1.name like "M")
                select(table._1.fetchBy {
                    allScalarFields()
                })
            }
        ) {

        }
    }
}