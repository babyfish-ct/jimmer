package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.ast.expression.nullValue
import org.babyfish.jimmer.sql.kt.ast.expression.value
import org.babyfish.jimmer.sql.kt.ast.query.baseTableSymbol
import org.babyfish.jimmer.sql.kt.ast.table.sourceId
import org.babyfish.jimmer.sql.kt.ast.table.targetId
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.createInsert
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.babyfish.jimmer.sql.kt.query.tuple.AggregateTupleMapper
import java.math.BigDecimal
import kotlin.test.Test

class InsertFromSelectDslTest : AbstractMutationTest() {

    @Test
    fun testRootlessTypedTupleInsert() {
        val client = sqlClient {
            setDialect(H2Dialect())
        }
        val source = baseTableSymbol {
            client.createBaseQuery {
                select(
                    AggregateTupleMapper
                        .storeId(value(9_999L))
                        .bookCount(value(0L))
                        .minPrice(nullValue<BigDecimal>())
                        .maxPrice(nullValue<BigDecimal>())
                        .avgPrice(nullValue<BigDecimal>())
                )
            }
        }
        executeAndExpectRowCount(
            client.createInsert<BookStore, _>(source) {
                set(table.id, sourceTable.storeId)
                set(table.name, value("KOTLIN-ROOTLESS"))
            }
        ) {
            statement {
                sql(
                    "insert into BOOK_STORE(ID, NAME, VERSION) " +
                            "select tb_1_.c1, ?, ? from (" +
                            "select cast(? as bigint) as c1" +
                            ") tb_1_"
                )
            }
            rowCount(1)
        }
    }

    @Test
    fun testAssociationInsertDsl() {
        val client = sqlClient {
            setDialect(H2Dialect())
        }
        val source = baseTableSymbol {
            client.createBaseQuery {
                select(
                    AggregateTupleMapper
                        .storeId(value(3L))
                        .bookCount(value(5L))
                        .minPrice(nullValue<BigDecimal>())
                        .maxPrice(nullValue<BigDecimal>())
                        .avgPrice(nullValue<BigDecimal>())
                )
            }
        }
        executeAndExpectRowCount(
            client.createInsert(Book::authors, source) {
                set(table.sourceId, sourceTable.storeId)
                set(table.targetId, sourceTable.bookCount)
            }
        ) {
            statement {
                sql(
                    "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                            "select tb_1_.c1, tb_1_.c2 from (" +
                            "select cast(? as bigint) as c1, cast(? as bigint) as c2" +
                            ") tb_1_"
                )
            }
            rowCount(1)
        }
    }

    @Test
    fun testInsertReturningDsl() {
        val client = sqlClient {
            setDialect(H2Dialect())
        }
        val source = baseTableSymbol {
            client.createBaseQuery {
                select(
                    AggregateTupleMapper
                        .storeId(value(9_998L))
                        .bookCount(value(0L))
                        .minPrice(nullValue<BigDecimal>())
                        .maxPrice(nullValue<BigDecimal>())
                        .avgPrice(nullValue<BigDecimal>())
                )
            }
        }
        connectAndExpect({ con ->
            client.createInsertReturning(BookStore::class, source) {
                set(table.id, sourceTable.storeId)
                set(table.name, value("KOTLIN-RETURNING"))
                returning(table.id)
            }.execute(con)
        }) {
            statement {
                sql(
                    "select ID from final table (" +
                            "insert into BOOK_STORE(ID, NAME, VERSION) " +
                            "select tb_1_.c1, ?, ? from (" +
                            "select cast(? as bigint) as c1" +
                            ") tb_1_" +
                            ")"
                )
            }
            value("[9998]")
        }
    }
}
