package org.babyfish.jimmer.sql.example.bll.resolver

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.lang.Ref
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
import java.util.*

@Component
class BookStoreNewestBooksResolver(
    private val bookStoreRepository: BookStoreRepository
) : KTransientResolver<Long, List<Long>> {

    override fun resolve(ids: Collection<Long>): Map<Long, List<Long>> =
        bookStoreRepository
            .findIdAndNewestBookId(ids)
            .groupBy({it._1}) {
                it._2
            }

    // -----------------------------
    // If you are a beginner, you can ignore all the following code.
    //
    // The following code is only used for cache mode(start the application
    // by `application.yml`).
    //
    // Unlike the fully automatic cache consistency maintenance of
    // ordinary associated property, if a calculated property uses cache,
    // its consistency requires manual assistance.
    // -----------------------------

    @EventListener
    fun onAssociationChange(e: AssociationEvent) {
        if (e.connection === null && e.immutableProp === BookStore::books.toImmutableProp()) {

            // 1. Check whether the association `BookStore.books` is changed,
            //    this event can be caused by 2 cases:
            //    i. The foreign key `Book.store.id` is changed.
            //    ii. The `TenantFilter` is enabled and the `Book.tenant` is changed.
            bookStoreRepository.sql.caches
                .getPropertyCache<Any, Any>(BookStore::newestBooks)
                ?.delete(e.sourceId)
        }
    }

    @EventListener
    fun onEntityChange(e: EntityEvent<*>) {
        if (e.connection === null && e.immutableType.javaClass == Book::class.java) {
            val storeId = e.getUnchangedFieldRef(Book::store)?.value?.id
            if (storeId !== null) {
                // 2. Otherwise, check whether `Book.edition` is changed.
                if (e.getChangedFieldRef(Book::edition) !== null) {
                    bookStoreRepository.sql.caches
                        .getPropertyCache<Any, Any>(BookStore::newestBooks)
                        ?.delete(storeId)
                }
            }
        }
    }

    // Contribute part of the secondary hash key to multiview-cache
    override fun getParameterMapRef(): Ref<SortedMap<String, Any>?>? {
        val filters = bookStoreRepository.sql.filters
        return filters.getTargetParameterMapRef(BookStore::books)
    }
}