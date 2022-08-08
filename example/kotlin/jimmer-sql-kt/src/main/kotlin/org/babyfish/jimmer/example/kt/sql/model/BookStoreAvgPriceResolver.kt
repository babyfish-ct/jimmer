package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.babyfish.jimmer.sql.kt.ast.expression.avg
import org.babyfish.jimmer.sql.kt.ast.expression.coalesce
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.toImmutableProp
import java.math.BigDecimal
import java.sql.Connection

class BookStoreAvgPriceResolver(
    private val sqlClient: KSqlClient
) : KTransientResolver<Long, BigDecimal> {

    init {

        // Unlike object caches and associative caches that can be automatically synchronized,
        // business computing caches require users to implement their synchronization logic.

        // 1. Check whether the association `BookStore.books` is changed.
        sqlClient.triggers.addAssociationListener(BookStore::books) {
            sqlClient.caches.getPropertyCache<Any, Any>(BOOK_STORE_DOT_AVG_PRICE)?.delete(it.sourceId)
        }

        sqlClient.triggers.addEntityListener(Book::class) {
            val storeId = it.getUnchangedFieldRef<BookStore>(BOOK_DOT_STORE.id)?.value?.id
            if (storeId !== null) {
                // 2. Otherwise, check whether `Book.price` is changed.
                if (it.getUnchangedFieldRef<BigDecimal>(BOOK_DOT_PRICE.id) === null) {
                    sqlClient.caches.getPropertyCache<Any, Any>(BOOK_STORE_DOT_AVG_PRICE)?.delete(storeId)
                }
            }
        }
    }

    override fun resolve(
        ids: Collection<Long>,
        con: Connection
    ): Map<Long, BigDecimal> =
        sqlClient
            .createQuery(Book::class) {
                where(table.store.id valueIn ids)
                groupBy(table.store.id)
                select(
                    table.store.id,
                    avg(table.price).coalesce(BigDecimal.ZERO)
                )
            }
            .execute(con) // It's important to specify the connection
            .associateBy({
                it._1
            }) {
                it._2
            }

    companion object {

        private val BOOK_STORE_DOT_AVG_PRICE = BookStore::avgPrice.toImmutableProp()

        private val BOOK_DOT_STORE = Book::store.toImmutableProp()

        private val BOOK_DOT_PRICE = Book::price.toImmutableProp()
    }
}