package org.babyfish.jimmer.sql.example.bll.resolver

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.sql.event.AssociationEvent
import org.babyfish.jimmer.sql.event.EntityEvent
import org.babyfish.jimmer.sql.example.dal.BookStoreRepository
import org.babyfish.jimmer.sql.example.model.Book
import org.babyfish.jimmer.sql.example.model.BookStore
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.babyfish.jimmer.sql.kt.event.getChangedFieldRef
import org.babyfish.jimmer.sql.kt.event.getUnchangedFieldRef
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class BookStoreAvgPriceResolver(
    private val bookStoreRepository: BookStoreRepository
) : KTransientResolver<Long, BigDecimal> {

    override fun resolve(ids: Collection<Long>): Map<Long, BigDecimal> =
        bookStoreRepository
            .findIdAndAvgBookPrice(ids)
            .associateBy({it._1}) {
                it._2
            }

    @EventListener
    fun onAssociationChange(e: AssociationEvent) {
        if (e.connection === null && e.immutableProp === BookStore::books.toImmutableProp()) {

            // 1. Check whether the association `BookStore.books` is changed,
            //    this event includes 2 cases:
            //    i. The foreign key of book is changed.
            //    ii. The `TenantFilter` is enabled and the tenant of book is changed.
            bookStoreRepository.sql.caches
                .getPropertyCache<Any, Any>(BookStore::avgPrice)
                ?.delete(e.sourceId)
        }
    }

    @EventListener
    fun onEntityChange(e: EntityEvent<*>) {
        if (e.connection === null && e.immutableType.javaClass == Book::class.java) {
            val storeId = e.getUnchangedFieldRef(Book::store)?.value?.id
            if (storeId !== null) {
                // 2. Otherwise, check whether `Book.price` is changed.
                if (e.getChangedFieldRef(Book::price) !== null) {
                    bookStoreRepository.sql.caches
                        .getPropertyCache<Any, Any>(BookStore::avgPrice)
                        ?.delete(storeId)
                }
            }
        }
    }
}