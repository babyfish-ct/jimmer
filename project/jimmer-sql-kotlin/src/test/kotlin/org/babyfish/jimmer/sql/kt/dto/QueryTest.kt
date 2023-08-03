package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.book.dto.BookView
import org.babyfish.jimmer.sql.kt.model.classic.book.edition
import org.babyfish.jimmer.sql.kt.model.classic.book.name
import org.junit.Test

class QueryTest : AbstractQueryTest() {

    @Test
    fun testBookView() {
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                where(table.name eq "GraphQL in Action")
                orderBy(table.edition.desc())
                select(
                    table.fetch(BookView::class)
                )
            }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.STORE_ID " +
                    "from BOOK tb_1_ " +
                    "where tb_1_.NAME = ? " +
                    "order by tb_1_.EDITION desc"
            )
            statement(1).sql(
                "select tb_1_.ID, tb_1_.NAME " +
                    "from BOOK_STORE tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            statement(2).sql(
                "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                    "from AUTHOR tb_1_ " +
                    "inner join BOOK_AUTHOR_MAPPING tb_2_ " +
                    "--->on tb_1_.ID = tb_2_.AUTHOR_ID " +
                    "where tb_2_.BOOK_ID in (?, ?, ?)"
            )
            rows {
                assertContentEquals(
                    """[
                        |--->BookView(
                        |--->--->name=GraphQL in Action, 
                        |--->--->edition=3, 
                        |--->--->id=12, 
                        |--->--->store=TargetOf_store(name=MANNING), 
                        |--->--->authors=[
                        |--->--->--->TargetOf_authors(firstName=Samer, lastName=Buna)
                        |--->--->]
                        |--->), 
                        |--->BookView(
                        |--->--->name=GraphQL in Action, 
                        |--->--->edition=2, 
                        |--->--->id=11, 
                        |--->--->store=TargetOf_store(name=MANNING), 
                        |--->--->authors=[
                        |--->--->--->TargetOf_authors(firstName=Samer, lastName=Buna)
                        |--->--->]
                        |--->), 
                        |--->BookView(
                        |--->--->name=GraphQL in Action, 
                        |--->--->edition=1, 
                        |--->--->id=10, 
                        |--->--->store=TargetOf_store(name=MANNING), 
                        |--->--->authors=[
                        |--->--->--->TargetOf_authors(firstName=Samer, lastName=Buna)
                        |--->--->]
                        |--->)
                        |]""".trimMargin(),
                    it
                )
            }
        }
    }
}