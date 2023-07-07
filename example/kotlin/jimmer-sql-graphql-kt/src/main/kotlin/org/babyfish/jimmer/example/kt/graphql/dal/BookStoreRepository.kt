package org.babyfish.jimmer.example.kt.graphql.dal

import org.babyfish.jimmer.example.kt.graphql.entities.*
import org.babyfish.jimmer.spring.repository.KRepository

interface BookStoreRepository : KRepository<BookStore, Long> {

    fun findByNameLikeOrderByName(name: String?): List<BookStore>
}