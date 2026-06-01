package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.model.classic.author.books
import org.babyfish.jimmer.sql.kt.model.classic.author.firstName
import org.babyfish.jimmer.sql.kt.model.classic.author.lastName
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import org.babyfish.jimmer.sql.kt.model.classic.book.name
import kotlin.test.Test

class MergeTest : AbstractQueryTest() {

    @Test
    fun testSubQuery() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(
                    exists(
                        subQuery(Author::class) {
                            where(
                                table.books eq parentTable,
                                table.firstName ilike "e"
                            )
                            select(constant(1))
                        } minus
                        subQuery(Author::class) {
                            where(
                                table.books eq parentTable,
                                table.lastName eq "Banks"
                            )
                            select(constant(1))
                        }
                    )
                )
                orderBy(table.name.asc(), table.edition.desc())
                select(table)
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID 
                    |from BOOK tb_1_ 
                    |where exists(
                    |--->(
                    |--->--->select 1 
                    |--->--->from AUTHOR tb_2_ 
                    |--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_2_.ID = tb_4_.AUTHOR_ID 
                    |--->--->where 
                    |--->--->--->tb_4_.BOOK_ID = tb_1_.ID 
                    |--->--->and 
                    |--->--->--->lower(tb_2_.FIRST_NAME) like ?
                    |--->) minus (
                    |--->--->select 1 
                    |--->--->from AUTHOR tb_3_ 
                    |--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_3_.ID = tb_5_.AUTHOR_ID 
                    |--->--->where 
                    |--->--->--->tb_5_.BOOK_ID = tb_1_.ID 
                    |--->--->and 
                    |--->--->--->tb_3_.LAST_NAME = ?
                    |--->)
                    |) order by tb_1_.NAME asc, tb_1_.EDITION desc""".trimMargin()
            )
            rows(
                "[" +
                    "--->{" +
                    "--->--->\"id\":12," +
                    "--->--->\"name\":\"GraphQL in Action\"," +
                    "--->--->\"edition\":3," +
                    "--->--->\"price\":80.00," +
                    "--->--->\"storeId\":2" +
                    "--->}," +
                    "--->{" +
                    "--->--->\"id\":11," +
                    "--->--->\"name\":\"GraphQL in Action\"," +
                    "--->--->\"edition\":2," +
                    "--->--->\"price\":81.00," +
                    "--->--->\"storeId\":2" +
                    "--->}," +
                    "--->{" +
                    "--->--->\"id\":10," +
                    "--->--->\"name\":\"GraphQL in Action\"," +
                    "--->--->\"edition\":1," +
                    "--->--->\"price\":80.00," +
                    "--->--->\"storeId\":2" +
                    "--->}" +
                    "]"
            )
        }
    }
}