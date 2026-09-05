package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.*
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.KSelectionExecutable
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.nullValue
import org.babyfish.jimmer.sql.kt.ast.expression.value
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableInsert
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpsert
import org.babyfish.jimmer.sql.kt.ast.query.baseTableSymbol
import org.babyfish.jimmer.sql.kt.ast.table.sourceId
import org.babyfish.jimmer.sql.kt.ast.table.targetId
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.store.id
import org.babyfish.jimmer.sql.kt.model.classic.store.name
import org.babyfish.jimmer.sql.kt.model.classic.store.website
import org.babyfish.jimmer.sql.kt.query.tuple.AggregateTupleMapper
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InsertFromSelectApiShapeTest : AbstractMutationTest() {

    @Test
    fun testEntityFactoryReturnTypesAndDslSeparation() {
        val client = sqlClient { setDialect(H2Dialect()) }
        val source = source(client)

        val insert: KExecutable<Int> = client.createInsert<BookStore, _>(source) {
            set(table.id, sourceTable.storeId)
            set(table.name, value("INSERT"))
            onConflictDoNothing(table.id)
        }
        val insertByClass: KExecutable<Int> = client.createInsert(BookStore::class, source) {
            set(table.id, sourceTable.storeId)
            set(table.name, value("INSERT-CLASS"))
        }
        val insertReturning: KSelectionExecutable<Long> =
            client.createInsertReturning<BookStore, _, Long>(source) {
                set(table.id, sourceTable.storeId)
                set(table.name, value("INSERT-RETURNING"))
                returning(table.id)
            }

        val upsert: KExecutable<Int> = client.createUpsert<BookStore, _>(source) {
            key(table.id, sourceTable.storeId)
            merge(table.name, value("UPSERT"))
            update(table.website, value("UPDATED"))
            updateWhere(table.name eq "OLD")
        }
        val upsertReturning: KSelectionExecutable<Long> =
            client.createUpsertReturning<BookStore, _, Long>(source) {
                key(table.id, sourceTable.storeId)
                merge(table.name, value("UPSERT-RETURNING"))
                update(table.website, nullValue<String>())
                returning(table.id)
            }

        val executeInsertFactory: () -> Int = {
            client.executeInsert<BookStore, _>(source) {
                set(table.id, sourceTable.storeId)
                set(table.name, value("EXECUTE-INSERT"))
            }
        }
        val executeInsertReturningFactory: () -> List<Long> = {
            client.executeInsertReturning<BookStore, _, Long>(source) {
                set(table.id, sourceTable.storeId)
                set(table.name, value("EXECUTE-INSERT-RETURNING"))
                returning(table.id)
            }
        }
        val executeUpsertFactory: () -> Int = {
            client.executeUpsert<BookStore, _>(source) {
                key(table.id, sourceTable.storeId)
                merge(table.name, value("EXECUTE-UPSERT"))
            }
        }
        val executeUpsertReturningFactory: () -> List<Long> = {
            client.executeUpsertReturning<BookStore, _, Long>(source) {
                key(table.id, sourceTable.storeId)
                merge(table.name, value("EXECUTE-UPSERT-RETURNING"))
                returning(table.id)
            }
        }

        assertNotNull(insert)
        assertNotNull(insertByClass)
        assertNotNull(insertReturning)
        assertNotNull(upsert)
        assertNotNull(upsertReturning)
        assertNotNull(executeInsertFactory)
        assertNotNull(executeInsertReturningFactory)
        assertNotNull(executeUpsertFactory)
        assertNotNull(executeUpsertReturningFactory)

        val insertMethods = KMutableInsert::class.java.methods.map { it.name }.toSet()
        assertTrue("onConflictDoNothing" in insertMethods)
        assertFalse("key" in insertMethods)
        assertFalse("merge" in insertMethods)
        assertFalse("update" in insertMethods)
        assertFalse("updateWhere" in insertMethods)

        val upsertMethods = KMutableUpsert::class.java.methods.map { it.name }.toSet()
        assertTrue("key" in upsertMethods)
        assertTrue("merge" in upsertMethods)
        assertTrue("update" in upsertMethods)
        assertTrue("updateWhere" in upsertMethods)
        assertFalse("onConflictDoNothing" in upsertMethods)
    }

    @Test
    fun testAssociationOverloadShapeForListAndReference() {
        val client = sqlClient { setDialect(H2Dialect()) }
        val source = source(client)

        val listBaseQuery = client.createBaseQuery(Book::authors) {
            selections.add(table.sourceId)
        }

        val listInsert: KExecutable<Int> = client.createInsert(Book::authors, source) {
            set(table.sourceId, sourceTable.storeId)
            set(table.targetId, sourceTable.bookCount)
        }
        val listReturning: KSelectionExecutable<Any> =
            client.createInsertReturning(Book::authors, source) {
                set(table.sourceId, sourceTable.storeId)
                set(table.targetId, sourceTable.bookCount)
                returning(table.sourceId)
            }
        val executeListInsertFactory: () -> Int = {
            client.executeInsert(Book::authors, source) {
                set(table.sourceId, sourceTable.storeId)
                set(table.targetId, sourceTable.bookCount)
            }
        }
        val executeListReturningFactory: () -> List<Any> = {
            client.executeInsertReturning(Book::authors, source) {
                set(table.sourceId, sourceTable.storeId)
                set(table.targetId, sourceTable.bookCount)
                returning(table.sourceId)
            }
        }

        // The shared Kotlin test model has no single-valued association based on a
        // middle table. Keep these factories uninvoked: Their purpose is to make
        // the compiler verify the reference-property overloads and return types.
        val referenceBaseFactory: () -> Any = {
            client.createBaseQuery(Book::store) {
                selections.add(table.sourceId)
            }
        }
        val referenceInsertFactory: () -> KExecutable<Int> = {
            client.createInsert(Book::store, source) {
                set(table.sourceId, sourceTable.storeId)
                set(table.targetId, sourceTable.bookCount)
            }
        }
        val referenceReturningFactory: () -> KSelectionExecutable<Any> = {
            client.createInsertReturning(Book::store, source) {
                set(table.sourceId, sourceTable.storeId)
                set(table.targetId, sourceTable.bookCount)
                returning(table.sourceId)
            }
        }
        val executeReferenceInsertFactory: () -> Int = {
            client.executeInsert(Book::store, source) {
                set(table.sourceId, sourceTable.storeId)
                set(table.targetId, sourceTable.bookCount)
            }
        }
        val executeReferenceReturningFactory: () -> List<Any> = {
            client.executeInsertReturning(Book::store, source) {
                set(table.sourceId, sourceTable.storeId)
                set(table.targetId, sourceTable.bookCount)
                returning(table.sourceId)
            }
        }

        assertNotNull(listBaseQuery)
        assertNotNull(listInsert)
        assertNotNull(listReturning)
        assertNotNull(executeListInsertFactory)
        assertNotNull(executeListReturningFactory)
        assertNotNull(referenceBaseFactory)
        assertNotNull(referenceInsertFactory)
        assertNotNull(referenceReturningFactory)
        assertNotNull(executeReferenceInsertFactory)
        assertNotNull(executeReferenceReturningFactory)
    }

    private fun source(client: org.babyfish.jimmer.sql.kt.KSqlClient) = baseTableSymbol {
        client.createBaseQuery {
            select(
                AggregateTupleMapper
                    .storeId(value(9_997L))
                    .bookCount(value(9_996L))
                    .minPrice(nullValue<BigDecimal>())
                    .maxPrice(nullValue<BigDecimal>())
                    .avgPrice(nullValue<BigDecimal>())
            )
        }
    }
}
