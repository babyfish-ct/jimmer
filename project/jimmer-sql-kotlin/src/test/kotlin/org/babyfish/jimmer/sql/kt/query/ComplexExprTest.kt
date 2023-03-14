package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.*
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import org.babyfish.jimmer.sql.kt.model.classic.book.name
import org.babyfish.jimmer.sql.kt.model.classic.book.price
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.babyfish.jimmer.sql.kt.model.classic.store.website
import java.math.BigDecimal
import kotlin.test.Test

class ComplexExprTest : AbstractQueryTest() {

    @Test
    fun testSimpleCase() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                select(
                    case(table.name)
                        .match("O'REILLY", "O")
                        .match("MANNING", "M")
                        .otherwise("-")
                )
            }
        ) {
            sql(
                """select 
                    |--->case tb_1_.NAME 
                    |--->--->when ? then ? 
                    |--->--->when ? then ? 
                    |--->--->else ? 
                    |--->end 
                    |from BOOK_STORE as tb_1_""".trimMargin()
            )
            variables("O'REILLY", "O", "MANNING", "M", "-")
            rows("""["M","O"]""")
        }
    }

    @Test
    fun testCase() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.name ilike "TYPEscript")
                orderBy(table.name)
                orderBy(table.edition.desc())
                select(
                    table.name,
                    case()
                        .match(table.price lt BigDecimal(40), "CHEAP")
                        .match(table.price gt BigDecimal(60), "EXPENSIVE")
                        .otherwise("MIDDLE")
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.NAME, 
                    |--->case 
                    |--->--->when tb_1_.PRICE < ? then ? 
                    |--->--->when tb_1_.PRICE > ? then ? 
                    |--->--->else ? 
                    |--->end 
                    |from BOOK as tb_1_ 
                    |where 
                    |--->lower(tb_1_.NAME) like ? 
                    |order by 
                    |--->tb_1_.NAME asc, 
                    |--->tb_1_.EDITION desc""".trimMargin()
            )
            variables(BigDecimal(40), "CHEAP", BigDecimal(60), "EXPENSIVE", "MIDDLE", "%typescript%")
            rows {
                contentEquals(
                    """[
                        |--->Tuple2(_1=Effective TypeScript, _2=EXPENSIVE), 
                        |--->Tuple2(_1=Effective TypeScript, _2=EXPENSIVE), 
                        |--->Tuple2(_1=Effective TypeScript, _2=EXPENSIVE), 
                        |--->Tuple2(_1=Programming TypeScript, _2=MIDDLE), 
                        |--->Tuple2(_1=Programming TypeScript, _2=MIDDLE), 
                        |--->Tuple2(_1=Programming TypeScript, _2=MIDDLE)
                        |]""".trimMargin(),
                    it.toString()
                )
            }
        }
    }

    @Test
    fun test() {
        executeAndExpect(
            sqlClient.createQuery(BookStore::class) {
                select(table.website.coalesce("DEFAULT_URL"))
            }
        ) {
            sql(
                """select coalesce(tb_1_.WEBSITE, ?) from BOOK_STORE as tb_1_"""
            )
            variables("DEFAULT_URL")
            rows("""["DEFAULT_URL","DEFAULT_URL"]""")
        }
    }
}