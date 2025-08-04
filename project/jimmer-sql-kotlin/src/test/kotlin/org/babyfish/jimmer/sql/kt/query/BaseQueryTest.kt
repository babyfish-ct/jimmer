package org.babyfish.jimmer.sql.kt.query

import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.query.baseTableSymbol
import org.babyfish.jimmer.sql.kt.ast.table.*
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.classic.author.*
import org.babyfish.jimmer.sql.kt.model.classic.book.*
import org.babyfish.jimmer.sql.kt.model.classic.store.*
import org.babyfish.jimmer.sql.kt.model.embedded.Transform
import org.babyfish.jimmer.sql.kt.model.embedded.fetchBy
import org.babyfish.jimmer.sql.kt.model.embedded.id
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BaseQueryTest : AbstractQueryTest() {

    @Test
    fun testBaseQueryWithFetch() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(BookStore::class) {
                selections
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
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 le 2)
                where(table._1.name like "M")
                select(table._1.fetchBy {
                    allScalarFields()
                    books {
                        allScalarFields()
                    }
                })
            }
        ) {
            sql(
                """select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4 
                    |from (
                    |--->select 
                    |--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.VERSION c3, tb_2_.WEBSITE c4, 
                    |--->--->dense_rank() over(
                    |--->--->--->order by (
                    |--->--->--->--->select count(1) from BOOK tb_3_ 
                    |--->--->--->--->where tb_3_.STORE_ID = tb_2_.ID
                    |--->--->--->) desc
                    |--->--->) c5 
                    |--->from BOOK_STORE tb_2_
                    |) tb_1_ 
                    |where tb_1_.c5 <= ? and tb_1_.c2 like ?""".trimMargin()
            )
            statement(1).sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE 
                    |from BOOK tb_1_ where tb_1_.STORE_ID = ?""".trimMargin()
            )
            rows(
                """[{
                    |--->"id":2,
                    |--->"name":"MANNING",
                    |--->"version":0,
                    |--->"website":null,
                    |--->"books":[
                    |--->--->{"id":10,"name":"GraphQL in Action","edition":1,"price":80.00},
                    |--->--->{"id":11,"name":"GraphQL in Action","edition":2,"price":81.00},
                    |--->{"id":12,"name":"GraphQL in Action","edition":3,"price":80.00}
                    |--->]
                    |}]""".trimMargin()
            )
        }
    }

    @Test
    fun testBaseQueryWithJoinFetch() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                selections
                    .add(table)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.id)
                            select(rowCount())
                        }
                    )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 gt 1)
                select(
                    table._1.fetchBy {
                        allScalarFields()
                        store(ReferenceFetchType.JOIN_ALWAYS) {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                "select " +
                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, " +
                    "--->tb_6_.ID, tb_6_.NAME, tb_6_.VERSION, tb_6_.WEBSITE " +
                    "--->from (" +
                    "--->--->select " +
                    "--->--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.EDITION c3, tb_2_.PRICE c4, tb_2_.STORE_ID c5, " +
                    "--->--->--->(" +
                    "--->--->--->--->select count(1) " +
                    "--->--->--->--->from AUTHOR tb_3_ " +
                    "--->--->--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID " +
                    "--->--->--->--->where tb_4_.BOOK_ID = tb_2_.ID" +
                    "--->--->--->) c6 " +
                    "--->--->from BOOK tb_2_" +
                    "--->) tb_1_ " +
                    "--->left join BOOK_STORE tb_6_ on tb_1_.c5 = tb_6_.ID " +
                    "--->where tb_1_.c6 > ?"
            )
            rows(
                "[" +
                    "--->{" +
                    "--->--->\"id\":1," +
                    "--->--->\"name\":\"Learning GraphQL\"," +
                    "--->--->\"edition\":1," +
                    "--->--->\"price\":50.00," +
                    "--->--->\"store\":{" +
                    "--->--->--->\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null}" +
                    "--->},{" +
                    "--->--->\"id\":2," +
                    "--->--->\"name\":\"Learning GraphQL\"," +
                    "--->--->\"edition\":2," +
                    "--->--->\"price\":55.00," +
                    "--->--->\"store\":{" +
                    "--->--->--->\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null}" +
                    "--->},{" +
                    "--->--->\"id\":3," +
                    "--->--->\"name\":\"Learning GraphQL\"," +
                    "--->--->\"edition\":3," +
                    "--->--->\"price\":51.00," +
                    "--->--->\"store\":{" +
                    "--->--->--->\"id\":1,\"name\":\"O'REILLY\",\"version\":0,\"website\":null" +
                    "--->--->}" +
                    "--->}" +
                    "]"
            )
        }
    }

    @Test
    fun testBaseJoinedTableWithTable() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(BookStore::class) {
                where(table.name eq "MANNING")
                where(table.asTableEx().books.edition eq 3)
                selections
                    .add(table.asTableEx().books)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.asTableEx().books.id)
                            select(rowCount())
                        }
                    )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 gt 0)
                select(table._1)
            }
        ) {
            sql(
                "select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5 " +
                    "from (" +
                    "--->select tb_3_.ID c1, tb_3_.NAME c2, tb_3_.EDITION c3, tb_3_.PRICE c4, tb_3_.STORE_ID c5, " +
                    "--->(" +
                    "--->--->select count(1) " +
                    "--->--->from AUTHOR tb_4_ " +
                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ " +
                    "--->--->--->on tb_4_.ID = tb_5_.AUTHOR_ID " +
                    "--->--->where tb_5_.BOOK_ID = tb_3_.ID" +
                    "--->) c6 " +
                    "--->from BOOK_STORE tb_2_ " +
                    "--->inner join BOOK tb_3_ " +
                    "--->--->on tb_2_.ID = tb_3_.STORE_ID " +
                    "--->where tb_2_.NAME = ? and tb_3_.EDITION = ?" +
                    ") tb_1_ where tb_1_.c6 > ?"
            )
            rows(
                "[{" +
                    "--->\"id\":12," +
                    "--->\"name\":\"GraphQL in Action\"," +
                    "--->\"edition\":3," +
                    "--->\"price\":80.00," +
                    "--->\"storeId\":2" +
                    "}]"
            )
        }
    }

    @Test
    fun testBaseJoinedTableWithJoinFetch() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(BookStore::class) {
                where(table.name eq "MANNING")
                where(table.asTableEx().books.edition eq 3)
                selections
                    .add(table.asTableEx().books)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.asTableEx().books.id)
                            select(rowCount())
                        }
                    )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 gt 0)
                select(
                    table._1.fetchBy {
                        allScalarFields()
                        store(ReferenceFetchType.JOIN_ALWAYS) {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, 
                    |--->tb_7_.ID, tb_7_.NAME, tb_7_.VERSION, tb_7_.WEBSITE 
                    |from (
                    |--->select 
                    |--->--->tb_3_.ID c1, tb_3_.NAME c2, tb_3_.EDITION c3, tb_3_.PRICE c4, tb_3_.STORE_ID c5, 
                    |--->--->(
                    |--->--->--->select count(1) from AUTHOR tb_4_ 
                    |--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_4_.ID = tb_5_.AUTHOR_ID 
                    |--->--->--->where tb_5_.BOOK_ID = tb_3_.ID
                    |--->--->) c6 
                    |--->from BOOK_STORE tb_2_ 
                    |--->inner join BOOK tb_3_ on tb_2_.ID = tb_3_.STORE_ID 
                    |--->where tb_2_.NAME = ? and tb_3_.EDITION = ?
                    |) tb_1_ 
                    |left join BOOK_STORE tb_7_ on tb_1_.c5 = tb_7_.ID 
                    |where tb_1_.c6 > ?""".trimMargin()
            )
            rows(
                "[{" +
                    "--->\"id\":12," +
                    "--->\"name\":\"GraphQL in Action\"," +
                    "--->\"edition\":3," +
                    "--->\"price\":80.00," +
                    "--->\"store\":{" +
                    "--->--->\"id\":2," +
                    "--->--->\"name\":\"MANNING\"," +
                    "--->--->\"website\":null," +
                    "--->--->\"version\":0" +
                    "--->}" +
                    "}]"
            )
        }
    }

    @Test
    fun testMergedBaseQueryWithTable() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.name eq "Learning GraphQL")
                where(table.edition eq 3)
                selections
                    .add(table)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.id)
                            select(rowCount())
                        }
                    )
            } unionAll sqlClient.createBaseQuery(BookStore::class) {
                where(table.name eq "MANNING")
                where(table.asTableEx().books.edition eq 3)
                selections
                    .add(table.asTableEx().books)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.asTableEx().books.id)
                            select(rowCount())
                        }
                    )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2.gt(0))
                select(table._1)
            }
        ) {
            sql(
                "select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5 " +
                    "from (" +
                    "--->select " +
                    "--->--->tb_2_.ID c1, " +
                    "--->--->tb_2_.NAME c2, " +
                    "--->--->tb_2_.EDITION c3, " +
                    "--->--->tb_2_.PRICE c4, " +
                    "--->--->tb_2_.STORE_ID c5, " +
                    "--->--->(" +
                    "--->--->--->select count(1) " +
                    "--->--->--->from AUTHOR tb_3_ " +
                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID " +
                    "--->--->--->where tb_4_.BOOK_ID = tb_2_.ID" +
                    "--->--->) c6 " +
                    "--->from BOOK tb_2_ " +
                    "--->where tb_2_.NAME = ? and tb_2_.EDITION = ? " +
                    "--->union all " +
                    "--->select " +
                    "--->--->tb_7_.ID c1, " +
                    "--->--->tb_7_.NAME c2, " +
                    "--->--->tb_7_.EDITION c3, " +
                    "--->--->tb_7_.PRICE c4, " +
                    "--->--->tb_7_.STORE_ID c5, " +
                    "--->--->(" +
                    "--->--->--->select count(1) " +
                    "--->--->--->from AUTHOR tb_8_ " +
                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_9_ on tb_8_.ID = tb_9_.AUTHOR_ID " +
                    "--->--->--->where tb_9_.BOOK_ID = tb_7_.ID" +
                    "--->--->) c6 " +
                    "--->from BOOK_STORE tb_6_ " +
                    "--->inner join BOOK tb_7_ on tb_6_.ID = tb_7_.STORE_ID " +
                    "--->where tb_6_.NAME = ? and tb_7_.EDITION = ?" +
                    ") tb_1_ " +
                    "where tb_1_.c6 > ?"
            )
            rows(
                "[{" +
                    "--->\"id\":3," +
                    "--->\"name\":\"Learning GraphQL\"," +
                    "--->\"edition\":3," +
                    "--->\"price\":51.00," +
                    "--->\"storeId\":1" +
                    "},{" +
                    "--->\"id\":12," +
                    "--->\"name\":\"GraphQL in Action\"," +
                    "--->\"edition\":3," +
                    "--->\"price\":80.00," +
                    "--->\"storeId\":2" +
                    "}]"
            )
        }
    }

    @Test
    fun testMergedBaseQueryWithJoinFetch() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.name eq "Learning GraphQL")
                where(table.edition eq 3)
                selections
                    .add(table)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.id)
                            select(rowCount())
                        }
                    )
            } unionAll sqlClient.createBaseQuery(BookStore::class) {
                where(table.name eq "MANNING")
                where(table.asTableEx().books.edition eq 3)
                selections
                    .add(table.asTableEx().books)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.asTableEx().books.id)
                            select(rowCount())
                        }
                    )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 gt 0)
                select(
                    table._1.fetchBy {
                        allScalarFields()
                        store(ReferenceFetchType.JOIN_ALWAYS) {
                            allScalarFields()
                        }
                    }
                )
            }
        ) {
            sql(
                "select " +
                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, " +
                    "--->tb_11_.ID, tb_11_.NAME, tb_11_.VERSION, tb_11_.WEBSITE " +
                    "from (" +
                    "--->select " +
                    "--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.EDITION c3, tb_2_.PRICE c4, tb_2_.STORE_ID c5, " +
                    "--->--->(" +
                    "--->--->--->select count(1) " +
                    "--->--->--->from AUTHOR tb_3_ " +
                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ " +
                    "--->--->--->on tb_3_.ID = tb_4_.AUTHOR_ID where tb_4_.BOOK_ID = tb_2_.ID" +
                    "--->--->) c6 " +
                    "--->from BOOK tb_2_ " +
                    "--->where tb_2_.NAME = ? and tb_2_.EDITION = ? " +
                    "--->union all " +
                    "--->select " +
                    "--->--->tb_7_.ID c1, tb_7_.NAME c2, tb_7_.EDITION c3, tb_7_.PRICE c4, tb_7_.STORE_ID c5, " +
                    "--->--->(" +
                    "--->--->--->select count(1) " +
                    "--->--->--->from AUTHOR tb_8_ " +
                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_9_ on tb_8_.ID = tb_9_.AUTHOR_ID " +
                    "--->--->--->where tb_9_.BOOK_ID = tb_7_.ID" +
                    "--->--->) c6 " +
                    "--->from BOOK_STORE tb_6_ " +
                    "--->inner join BOOK tb_7_ on tb_6_.ID = tb_7_.STORE_ID " +
                    "--->where tb_6_.NAME = ? and tb_7_.EDITION = ?" +
                    ") tb_1_ " +
                    "left join BOOK_STORE tb_11_ on tb_1_.c5 = tb_11_.ID " +
                    "where tb_1_.c6 > ?"
            )
            rows(
                "[{" +
                    "--->\"id\":3," +
                    "--->\"name\":\"Learning GraphQL\"," +
                    "--->\"edition\":3," +
                    "--->\"price\":51.00," +
                    "--->\"store\":{" +
                    "--->--->\"id\":1," +
                    "--->--->\"name\":\"O'REILLY\"," +
                    "--->--->\"website\":null," +
                    "--->--->\"version\":0" +
                    "--->}" +
                    "},{" +
                    "--->\"id\":12," +
                    "--->\"name\":\"GraphQL in Action\"," +
                    "--->\"edition\":3," +
                    "--->\"price\":80.00," +
                    "--->\"store\":{" +
                    "--->--->\"id\":2," +
                    "--->--->\"name\":\"MANNING\"," +
                    "--->--->\"website\":null," +
                    "--->--->\"version\":0" +
                    "--->}" +
                    "}]"
            )
        }
    }

    @Test
    fun testMergedBaseQueryWithOutsideJoinFetchAndInsideJoin() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.name eq "Learning GraphQL")
                where(table.edition eq 3)
                selections
                    .add(table)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.id)
                            select(rowCount())
                        }
                    )
            } unionAll sqlClient.createBaseQuery(BookStore::class) {
                where(table.name eq "MANNING")
                where(table.asTableEx().books.edition eq 3)
                where(table.asTableEx().books.authors.gender eq Gender.MALE)
                selections
                    .add(table.asTableEx().books)
                    .add(
                        subQuery(Author::class) {
                            where(table.books.id eq parentTable.asTableEx().books.id)
                            select(rowCount())
                        }
                    )
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                where(table._2 gt 0)
                where(table._1.edition.between(1, 3))
                select(
                    table._1.fetchBy {
                        name()
                        store(ReferenceFetchType.JOIN_ALWAYS) {
                            name()
                        }
                    }
                )
            }
        ) {
            sql(
                "select " +
                    "tb_1_.c1, tb_1_.c2, tb_13_.ID, tb_13_.NAME " +
                    "from (" +
                    "--->select " +
                    "--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.STORE_ID c3, tb_2_.EDITION c5, " +
                    "--->--->(" +
                    "--->--->--->select count(1) " +
                    "--->--->--->from AUTHOR tb_3_ " +
                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID " +
                    "--->--->--->where tb_4_.BOOK_ID = tb_2_.ID" +
                    "--->--->) c4 " +
                    "--->from BOOK tb_2_ " +
                    "--->where tb_2_.NAME = ? and tb_2_.EDITION = ? " +
                    "--->union all " +
                    "--->select tb_7_.ID c1, tb_7_.NAME c2, tb_7_.STORE_ID c3, tb_7_.EDITION c5, " +
                    "--->(" +
                    "--->--->select count(1) " +
                    "--->--->from AUTHOR tb_10_ " +
                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_11_ on tb_10_.ID = tb_11_.AUTHOR_ID " +
                    "--->--->where tb_11_.BOOK_ID = tb_7_.ID" +
                    "--->) c4 " +
                    "--->from BOOK_STORE tb_6_ " +
                    "--->inner join BOOK tb_7_ on tb_6_.ID = tb_7_.STORE_ID " +
                    "--->inner join BOOK_AUTHOR_MAPPING tb_8_ on tb_7_.ID = tb_8_.BOOK_ID " +
                    "--->inner join AUTHOR tb_9_ on tb_8_.AUTHOR_ID = tb_9_.ID " +
                    "--->where tb_6_.NAME = ? and tb_7_.EDITION = ? and tb_9_.GENDER = ?" +
                    ") tb_1_ " +
                    "left join BOOK_STORE tb_13_ on tb_1_.c3 = tb_13_.ID " +
                    "where tb_1_.c4 > ? and (tb_1_.c5 between ? and ?)"
            )
            rows(
                "[{" +
                    "--->\"id\":3," +
                    "--->\"name\":\"Learning GraphQL\"," +
                    "--->\"store\":{" +
                    "--->--->\"id\":1," +
                    "--->--->\"name\":\"O'REILLY\"" +
                    "--->}" +
                    "},{" +
                    "--->\"id\":12," +
                    "--->\"name\":\"GraphQL in Action\"," +
                    "--->\"store\":{" +
                    "--->--->\"id\":2," +
                    "--->--->\"name\":\"MANNING\"" +
                    "--->}" +
                    "}]"
            )
        }
    }

    @Test
    fun testSqlFormula() {
        var baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 2L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 4L)
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                select(
                    table._1.fetchBy {
                        fullName2()
                    }
                )
            }
        ) {
            sql(
                "select tb_1_.c1, tb_1_.c2 from (" +
                    "--->select tb_2_.ID c1, concat(tb_2_.FIRST_NAME, ' ', tb_2_.LAST_NAME) c2 " +
                    "--->from AUTHOR tb_2_ " +
                    "--->where tb_2_.ID = ? " +
                    "--->union all " +
                    "--->select tb_3_.ID c1, concat(tb_3_.FIRST_NAME, ' ', tb_3_.LAST_NAME) c2 " +
                    "--->from AUTHOR tb_3_ " +
                    "--->where tb_3_.ID = ?" +
                    ") tb_1_"
            )
            rows(
                "[{" +
                    "--->\"id\":2," +
                    "--->\"fullName2\":\"Alex Banks\"" +
                    "},{" +
                    "--->\"id\":4," +
                    "--->\"fullName2\":\"Boris Cherny\"" +
                    "}]"
            )
        }
    }

    @Test
    fun testFetchDefaultEmbeddable() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Transform::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Transform::class) {
                where(table.id eq 2L)
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                select(
                    table._1.fetchBy {
                        source()
                        target()
                    }
                )
            }
        ) {
            sql(
                "select " +
                    "--->tb_1_.c1, " +
                    "--->tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, " +
                    "--->tb_1_.c6, tb_1_.c7, tb_1_.c8, tb_1_.c9 " +
                    "from (" +
                    "--->select " +
                    "--->--->tb_2_.ID c1, " +
                    "--->--->tb_2_.`LEFT` c2, tb_2_.TOP c3, tb_2_.`RIGHT` c4, tb_2_.BOTTOM c5, " +
                    "--->--->tb_2_.TARGET_LEFT c6, tb_2_.TARGET_TOP c7, tb_2_.TARGET_RIGHT c8, tb_2_.TARGET_BOTTOM c9 " +
                    "--->from TRANSFORM tb_2_ " +
                    "--->where tb_2_.ID = ? " +
                    "--->union all " +
                    "--->select " +
                    "--->--->tb_3_.ID c1, " +
                    "--->--->tb_3_.`LEFT` c2, tb_3_.TOP c3, tb_3_.`RIGHT` c4, tb_3_.BOTTOM c5, " +
                    "--->--->tb_3_.TARGET_LEFT c6, tb_3_.TARGET_TOP c7, tb_3_.TARGET_RIGHT c8, tb_3_.TARGET_BOTTOM c9 " +
                    "--->from TRANSFORM tb_3_ " +
                    "--->where tb_3_.ID = ?" +
                    ") tb_1_"
            )
            rows(
                "[{" +
                    "--->\"id\":1," +
                    "--->\"source\":{\"leftTop\":{\"x\":100,\"y\":120},\"rightBottom\":{\"x\":400,\"y\":320}}," +
                    "--->\"target\":{\"leftTop\":{\"x\":800,\"y\":600},\"rightBottom\":{\"x\":1400,\"y\":1000}}" +
                    "}]"
            )
        }
    }

    @Test
    fun testFetchShapedEmbeddable() {
        val baseTable = baseTableSymbol {
            sqlClient.createBaseQuery(Transform::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Transform::class) {
                where(table.id eq 2L)
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseTable) {
                select(
                    table._1.fetchBy {
                        source {
                            leftTop()
                        }
                        target {
                            rightBottom()
                        }
                    }
                )
            }
        ) {
            sql(
                "select " +
                    "--->tb_1_.c1, " +
                    "--->tb_1_.c2, tb_1_.c3, " +
                    "--->tb_1_.c4, tb_1_.c5 " +
                    "from (" +
                    "--->select " +
                    "--->--->tb_2_.ID c1, " +
                    "--->--->tb_2_.`LEFT` c2, tb_2_.TOP c3, " +
                    "--->--->tb_2_.TARGET_RIGHT c4, tb_2_.TARGET_BOTTOM c5 " +
                    "--->from TRANSFORM tb_2_ " +
                    "--->where tb_2_.ID = ? " +
                    "--->union all " +
                    "--->select " +
                    "--->--->tb_3_.ID c1, " +
                    "--->--->tb_3_.`LEFT` c2, tb_3_.TOP c3, " +
                    "--->--->tb_3_.TARGET_RIGHT c4, tb_3_.TARGET_BOTTOM c5 " +
                    "--->from TRANSFORM tb_3_ " +
                    "--->where tb_3_.ID = ?" +
                    ") tb_1_"
            )
            rows(
                "[{" +
                    "--->\"id\":1," +
                    "--->\"source\":{\"leftTop\":{\"x\":100,\"y\":120}}," +
                    "--->\"target\":{\"rightBottom\":{\"x\":1400,\"y\":1000}}" +
                    "}]"
            )
        }
    }

    @Test
    fun testWeakJoinBaseTable() {
        val baseBook = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table)
            }
        }
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 2L)
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseBook) {
                where(table.weakJoin(baseAuthor, BaseBookAuthorJoin::class)._1.firstName.isNotNull())
                select(
                    table._1,
                    table.weakJoin(baseAuthor, BaseBookAuthorJoin::class)._1
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, 
                    |--->tb_2_.c6, tb_2_.c7, tb_2_.c8, tb_2_.c9 
                    |from (
                    |--->select tb_3_.ID c1, tb_3_.NAME c2, tb_3_.EDITION c3, tb_3_.PRICE c4, tb_3_.STORE_ID c5 
                    |--->from BOOK tb_3_ 
                    |--->where tb_3_.ID = ? 
                    |--->union all 
                    |--->select tb_4_.ID c1, tb_4_.NAME c2, tb_4_.EDITION c3, tb_4_.PRICE c4, tb_4_.STORE_ID c5 
                    |--->from BOOK tb_4_ where tb_4_.ID = ?
                    |) tb_1_ 
                    |inner join BOOK_AUTHOR_MAPPING tb_7_ on tb_1_.c1 = tb_7_.BOOK_ID 
                    |inner join (
                    |--->select tb_5_.ID c6, tb_5_.FIRST_NAME c7, tb_5_.LAST_NAME c8, tb_5_.GENDER c9 
                    |--->from AUTHOR tb_5_ 
                    |--->where tb_5_.ID = ? 
                    |--->union all 
                    |--->select tb_6_.ID c6, tb_6_.FIRST_NAME c7, tb_6_.LAST_NAME c8, tb_6_.GENDER c9 
                    |--->from AUTHOR tb_6_ 
                    |--->where tb_6_.ID = ?
                    |) tb_2_ on tb_7_.AUTHOR_ID = tb_2_.c6 
                    |where tb_2_.c7 is not null""".trimMargin()
            )
        }
    }

    @Test
    fun testLambdaWeakJoinBaseTable() {
        val baseBook = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table)
            }
        }
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 1L)
                selections.add(table)
            } unionAll sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 2L)
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseBook) {
                where(
                    table.weakJoin(baseAuthor) {
                        source._1.asTableEx().authors eq target._1
                    }._1.firstName.isNotNull()
                )
                select(
                    table._1,
                    table.weakJoin(baseAuthor) {
                        source._1.asTableEx().authors eq target._1
                    }._1
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, 
                    |--->tb_2_.c6, tb_2_.c7, tb_2_.c8, tb_2_.c9 
                    |from (
                    |--->select tb_3_.ID c1, tb_3_.NAME c2, tb_3_.EDITION c3, tb_3_.PRICE c4, tb_3_.STORE_ID c5 
                    |--->from BOOK tb_3_ 
                    |--->where tb_3_.ID = ? 
                    |--->union all 
                    |--->select tb_4_.ID c1, tb_4_.NAME c2, tb_4_.EDITION c3, tb_4_.PRICE c4, tb_4_.STORE_ID c5 
                    |--->from BOOK tb_4_ where tb_4_.ID = ?
                    |) tb_1_ 
                    |inner join BOOK_AUTHOR_MAPPING tb_7_ on tb_1_.c1 = tb_7_.BOOK_ID 
                    |inner join (
                    |--->select tb_5_.ID c6, tb_5_.FIRST_NAME c7, tb_5_.LAST_NAME c8, tb_5_.GENDER c9 
                    |--->from AUTHOR tb_5_ 
                    |--->where tb_5_.ID = ? 
                    |--->union all 
                    |--->select tb_6_.ID c6, tb_6_.FIRST_NAME c7, tb_6_.LAST_NAME c8, tb_6_.GENDER c9 
                    |--->from AUTHOR tb_6_ 
                    |--->where tb_6_.ID = ?
                    |) tb_2_ on tb_7_.AUTHOR_ID = tb_2_.c6 
                    |where tb_2_.c7 is not null""".trimMargin()
            )
        }
    }

    @Test
    fun testLambdaWeakJoinOfTwoColumnBaseTableWithFetcher() {
        val baseBook = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table.price).add(table)
            }
        }
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 1L)
                selections.add(table.gender).add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseBook) {
                where(table._1 gt BigDecimal.ZERO)
                where += table.weakJoin(baseAuthor) {
                        source._2.asTableEx().authors eq target._2
                    }._1 eq Gender.MALE
                select(
                    table._2.fetchBy {
                        name()
                    },
                    table.weakJoin(baseAuthor) {
                        source._2.asTableEx().authors eq target._2
                    }._2.fetchBy {
                        fullName2()
                    }
                )
            }
        ) {
            sql(
                "select " +
                    "--->tb_1_.c1, tb_1_.c2, " +
                    "--->tb_2_.c3, tb_2_.c4 " +
                    "from (" +
                    "--->select " +
                    "--->--->tb_3_.PRICE c5, tb_3_.ID c1, tb_3_.NAME c2 " +
                    "--->from BOOK tb_3_ " +
                    "--->where tb_3_.ID = ?" +
                    ") tb_1_ " +
                    "inner join BOOK_AUTHOR_MAPPING tb_5_ " +
                    "--->on tb_1_.c1 = tb_5_.BOOK_ID " +
                    "inner join (" +
                    "--->select " +
                    "--->--->tb_4_.GENDER c6, " +
                    "--->--->tb_4_.ID c3, concat(tb_4_.FIRST_NAME, ' ', tb_4_.LAST_NAME) c4 " +
                    "--->from AUTHOR tb_4_ " +
                    "--->where tb_4_.ID = ?" +
                    ") tb_2_ " +
                    "--->on tb_5_.AUTHOR_ID = tb_2_.c3 " +
                    "where tb_1_.c5 > ? and tb_2_.c6 = ?"
            )
        }
    }

    @Test
    fun testLambdaWeakJoinOfTwoColumnUnionAllBaseTableWithFetcher() {
        val baseBook = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 1L)
                selections.add(table.price).add(table)
            } unionAll sqlClient.createBaseQuery(Book::class) {
                where(table.id eq 2L)
                selections.add(table.price).add(table)
            }
        }
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 1L)
                selections.add(table.gender).add(table)
            } unionAll sqlClient.createBaseQuery(Author::class) {
                where(table.id eq 2L)
                selections.add(table.gender).add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseBook) {
                where(table._1 gt BigDecimal.ZERO)
                where += table.weakJoin(baseAuthor) {
                    source._2.asTableEx().authors.id eq target._2.id
                }._1 eq Gender.MALE
                select(
                    table._2.fetchBy {
                        name()
                    },
                    table.weakJoin(baseAuthor) {
                        source._2.asTableEx().authors.id eq target._2.id
                    }._2.fetchBy {
                        fullName2()
                    }
                )
            }
        ) {
            sql(
                "select " +
                    "tb_1_.c1, tb_1_.c2, " +
                    "tb_2_.c3, tb_2_.c4 " +
                    "from (" +
                    "--->select " +
                    "--->--->tb_3_.PRICE c5, tb_3_.ID c1, tb_3_.NAME c2 " +
                    "--->from BOOK tb_3_ " +
                    "--->where tb_3_.ID = ? " +
                    "--->union all " +
                    "--->select " +
                    "--->--->tb_4_.PRICE c5, tb_4_.ID c1, tb_4_.NAME c2 " +
                    "--->from BOOK tb_4_ " +
                    "--->where tb_4_.ID = ?" +
                    ") tb_1_ " +
                    "inner join BOOK_AUTHOR_MAPPING tb_7_ " +
                    "--->on tb_1_.c1 = tb_7_.BOOK_ID " +
                    "inner join (" +
                    "--->select " +
                    "--->--->tb_5_.GENDER c6, tb_5_.ID c3, " +
                    "--->--->concat(tb_5_.FIRST_NAME, ' ', tb_5_.LAST_NAME) c4 " +
                    "--->from AUTHOR tb_5_ " +
                    "--->where tb_5_.ID = ? " +
                    "--->union all " +
                    "--->select " +
                    "--->--->tb_6_.GENDER c6, tb_6_.ID c3, " +
                    "--->--->concat(tb_6_.FIRST_NAME, ' ', tb_6_.LAST_NAME) c4 " +
                    "--->from AUTHOR tb_6_ " +
                    "--->where tb_6_.ID = ?" +
                    ") tb_2_ " +
                    "--->on tb_7_.AUTHOR_ID = tb_2_.c3 " +
                    "where tb_1_.c5 > ? " +
                    "and tb_2_.c6 = ?"
            )
        }
    }

    @Test
    fun testTableWeakJoinBaseTable() {
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.gender eq Gender.MALE)
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(Book::class) {
                select(
                    table,
                    table.asTableEx().weakJoin(baseAuthor) {
                        source.id eq target._1.asTableEx().books.id
                    }._1
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, 
                    |--->tb_2_.c1, tb_2_.c2, tb_2_.c3, tb_2_.c4 
                    |from BOOK tb_1_ 
                    |inner join (
                    |--->(
                    |--->--->select tb_3_.ID c1, tb_3_.FIRST_NAME c2, tb_3_.LAST_NAME c3, tb_3_.GENDER c4 
                    |--->--->from AUTHOR tb_3_ 
                    |--->--->where tb_3_.GENDER = ?
                    |--->) tb_2_ 
                    |--->inner join BOOK_AUTHOR_MAPPING tb_4_ 
                    |--->--->on tb_2_.c1 = tb_4_.AUTHOR_ID
                    |) on tb_1_.ID = tb_4_.BOOK_ID""".trimMargin()
            )
        }
    }

    @Test
    fun testIllegalTableWeakJoinBaseTable() {
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.gender eq Gender.MALE)
                selections.add(table)
            }
        }
        assertFailsWith(
            IllegalStateException::class,
            "Table join is disabled. " +
                "For the weak join operation from a regular table to a base table, " +
                "the strong join is not allowed on the regular table side (source side)"
        ) {
            executeAndExpect(
                sqlClient.createQuery(Book::class) {
                    select(
                        table,
                        table.asTableEx().weakJoin(baseAuthor) {
                            source.asTableEx().authors.id eq target._1.id
                        }._1
                    )
                }
            ) {}
        }
    }

    @Test
    fun testBaseTableWeakOuterJoin() {
        val baseBook = baseTableSymbol {
            sqlClient.createBaseQuery(Book::class) {
                where(table.id valueIn listOf(1L, 10L))
                selections.add(table)
            }
        }
        val baseAuthor = baseTableSymbol {
            sqlClient.createBaseQuery(Author::class) {
                where(table.id valueIn listOf(1L, 2L))
                selections.add(table)
            }
        }
        executeAndExpect(
            sqlClient.createQuery(baseBook) {
                select(
                    table._1,
                    table.weakOuterJoin(baseAuthor) {
                        source._1.id eq target._1.asTableEx().books.id
                    }._1
                )
            }
        ) {
            sql(
                """select 
                    |--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, 
                    |--->tb_2_.c6, tb_2_.c7, tb_2_.c8, tb_2_.c9 
                    |from (
                    |--->select tb_3_.ID c1, tb_3_.NAME c2, tb_3_.EDITION c3, tb_3_.PRICE c4, tb_3_.STORE_ID c5 
                    |--->from BOOK tb_3_ 
                    |--->where tb_3_.ID in (?, ?)
                    |) tb_1_ 
                    |left join (
                    |--->select tb_4_.ID c6, tb_4_.FIRST_NAME c7, tb_4_.LAST_NAME c8, tb_4_.GENDER c9 
                    |--->from AUTHOR tb_4_ 
                    |--->where tb_4_.ID in (?, ?)
                    |) tb_2_ 
                    |inner join BOOK_AUTHOR_MAPPING tb_5_ 
                    |--->on tb_2_.c6 = tb_5_.AUTHOR_ID 
                    |on tb_1_.c1 = tb_5_.BOOK_ID""".trimMargin()
            )
            rows(
                """[{
                    |--->"_1":{"id":1,"name":"Learning GraphQL","edition":1,"price":50.0,"storeId":1},
                    |--->"_2":{"id":1,"firstName":"Eve","lastName":"Procello","gender":"FEMALE"}
                    |},{
                    |--->"_1":{"id":1,"name":"Learning GraphQL","edition":1,"price":50.0,"storeId":1},
                    |--->"_2":{"id":2,"firstName":"Alex","lastName":"Banks","gender":"MALE"}
                    |},{
                    |--->"_1":{"id":10,"name":"GraphQL in Action","edition":1,"price":80.0,"storeId":2},
                    |--->"_2":null
                    |}]""".trimMargin()
            )
        }
    }

    private class BaseBookAuthorJoin : KPropsWeakJoin<
        KNonNullBaseTable1<KNonNullTable<Book>, KNullableTable<Book>>,
        KNonNullBaseTable1<KNonNullTable<Author>, KNullableTable<Author>>
        >() {
        override fun on(
            source: KNonNullBaseTable1<KNonNullTable<Book>, KNullableTable<Book>>,
            target: KNonNullBaseTable1<KNonNullTable<Author>, KNullableTable<Author>>
        ): KNonNullExpression<Boolean> =
            source._1.asTableEx().authors.eq(target._1)
    }
}