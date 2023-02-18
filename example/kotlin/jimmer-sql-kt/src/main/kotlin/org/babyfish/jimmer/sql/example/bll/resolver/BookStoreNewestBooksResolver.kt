package org.babyfish.jimmer.sql.example.bll.resolver

import org.babyfish.jimmer.sql.example.dal.BookStoreRepository
import org.babyfish.jimmer.sql.kt.KTransientResolver
import org.springframework.stereotype.Component

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
}