package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import org.babyfish.jimmer.sql.kt.model.classic.book.fetchBy
import org.babyfish.jimmer.sql.kt.model.classic.book.name
import org.babyfish.jimmer.sql.kt.model.classic.book.store
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import kotlin.test.Test

class OffsetOptimizationTest : AbstractQueryTest() {

    @Test
    fun testBySelf() {
        executeAndExpect(
            sqlClient {
                setOffsetOptimizingThreshold(1)
            }.createQuery(Book::class) {
                where(table.store.name eq "O'REILLY")
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }.limit(3, 3)
        ) {
            sql(
                """select 
                    |--->optimize_.ID, 
                    |--->optimize_.NAME, 
                    |--->optimize_.EDITION, 
                    |--->optimize_.PRICE, 
                    |--->optimize_.STORE_ID 
                    |from (
                    |--->select 
                    |--->--->tb_1_.ID optimize_core_id_ 
                    |--->from BOOK tb_1_ 
                    |--->inner join BOOK_STORE tb_2_ 
                    |--->--->on tb_1_.STORE_ID = tb_2_.ID 
                    |--->where tb_2_.NAME = ? 
                    |--->order by tb_1_.NAME asc, tb_1_.EDITION desc 
                    |--->limit ? offset ?
                    |) optimize_core_ 
                    |inner join BOOK optimize_ on 
                    |--->optimize_.ID = optimize_core_.optimize_core_id_""".trimMargin()
            )
        }
    }

    @Test
    fun testBySelfWithAssociations() {
        executeAndExpect(
            sqlClient {
                setOffsetOptimizingThreshold(1)
            }.createQuery(Book::class) {
                where(table.store.name eq "O'REILLY")
                orderBy(table.name.asc(), table.edition.desc())
                select(table.fetchBy {
                    allScalarFields()
                    price(false)
                    authors {
                        allScalarFields()
                    }
                })
            }.limit(3, 3)
        ) {
            sql(
                """select 
                    |--->optimize_.ID, 
                    |--->optimize_.NAME, 
                    |--->optimize_.EDITION 
                    |from (
                    |--->select 
                    |--->--->tb_1_.ID optimize_core_id_ 
                    |--->from BOOK tb_1_ 
                    |--->inner join BOOK_STORE tb_2_ 
                    |--->--->on tb_1_.STORE_ID = tb_2_.ID 
                    |--->where tb_2_.NAME = ? 
                    |--->order by tb_1_.NAME asc, tb_1_.EDITION desc 
                    |--->limit ? offset ?
                    |) optimize_core_ 
                    |inner join BOOK optimize_ on 
                    |--->optimize_.ID = optimize_core_.optimize_core_id_""".trimMargin()
            )
            statement(1).sql(
                """select 
                    |--->tb_2_.BOOK_ID, 
                    |--->tb_1_.ID, 
                    |--->tb_1_.FIRST_NAME, 
                    |--->tb_1_.LAST_NAME, 
                    |--->tb_1_.GENDER 
                    |from AUTHOR tb_1_ 
                    |inner join BOOK_AUTHOR_MAPPING tb_2_ 
                    |--->on tb_1_.ID = tb_2_.AUTHOR_ID 
                    |where tb_2_.BOOK_ID in (?, ?, ?)""".trimMargin()
            )
        }
    }
}
