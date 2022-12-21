package org.babyfish.jimmer.example.kt.graphql.dal

import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.example.kt.graphql.entities.name
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.ilike

interface BookStoreRepository : KRepository<BookStore, Long> {

    fun find(name: String?): List<BookStore> =
        sql.createQuery(BookStore::class) {
            name?.let {
                where(table.name ilike it)
            }
            select(table)
        }.execute()
}