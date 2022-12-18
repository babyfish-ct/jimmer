package org.babyfish.jimmer.example.kt.sql.dal.calc

import org.babyfish.jimmer.example.kt.sql.model.*
import org.babyfish.jimmer.lang.Ref
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.babyfish.jimmer.sql.kt.ast.expression.asNonNull
import org.babyfish.jimmer.sql.kt.ast.expression.avg
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.event.getChangedFieldRef
import org.babyfish.jimmer.sql.kt.event.getUnchangedFieldRef
import java.math.BigDecimal
import java.sql.Connection
import java.util.*

class BookStoreAvgPriceResolver(
    private val sqlClient: KSqlClient
) : KTransientResolver<Long, BigDecimal> {

    init {

        // Unlike object caches and associative caches that can be automatically synchronized,
        // business computing caches require users to implement their synchronization logic.

        // 1. Check whether the association `BookStore.books` is changed,
        //    this event includes 2 cases:
        //    i. The foreign key of book is changed.
        //    ii. The `TenantFilter` is enabled and the tenant of book is changed.
        sqlClient.triggers.addAssociationListener(BookStore::books) {
            sqlClient.caches.getPropertyCache<Any, Any>(BookStore::avgPrice)?.delete(it.sourceId)
        }

        sqlClient.triggers.addEntityListener(Book::class) {
            val storeId = it.getUnchangedFieldRef(Book::store)?.value?.id
            if (storeId !== null) {
                // 2. Otherwise, check whether `Book.price` is changed.
                if (it.getChangedFieldRef(Book::price) !== null) {
                    sqlClient.caches.getPropertyCache<Any, Any>(BookStore::avgPrice)?.delete(storeId)
                }
            }
        }
    }

    override fun getParameterMapRef(): Ref<SortedMap<String, Any>?>? =
        sqlClient.filters.getTargetParameterMapRef(BookStore::books)

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
                    avg(table.price).asNonNull()
                )
            }
            .execute(con) // It's important to specify the connection
            .associateBy({
                it._1
            }) {
                it._2
            }
            .ensureKeys(ids, BigDecimal.ZERO)
}

private fun <K, V> Map<K, V>.ensureKeys(
    keys: Collection<K>,
    defaultValue: V
): Map<K, V> {
    val missedKeys = keys.toSet() - this.keys
    return if (missedKeys.isEmpty()) {
        this
    } else {
        toMutableMap().apply {
            for (missedKey in missedKeys) {
                this[missedKey] = defaultValue
            }
        }
    }
}