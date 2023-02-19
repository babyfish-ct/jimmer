package org.babyfish.jimmer.example.kt.graphql.bll

import org.babyfish.jimmer.example.kt.graphql.dal.BookStoreRepository
import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.example.kt.graphql.entities.input.BookStoreInput
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

/**
 * A real project should be a three-tier architecture consisting
 * of repository, service, and controller.
 *
 * This demo has no business logic, its purpose is only to tell users
 * how to use jimmer with the <b>least</b> code. Therefore, this demo
 * does not follow this convention, and let services be directly
 * decorated by `@Controller`, not `@Service`.
 */
@Controller
class BookStoreService(
    private val bookStoreRepository: BookStoreRepository
) {

    // --- Query ---

    @QueryMapping
    fun bookStores(
        @Argument name: String?
    ): List<BookStore> {
        return bookStoreRepository.findByNameLikeOrderByName(name)
    }

    // --- Mutation ---
    @MutationMapping
    @Transactional
    fun saveBookStore(@Argument input: BookStoreInput): BookStore =
        bookStoreRepository.save(input)

    @MutationMapping
    @Transactional
    fun deleteBookStore(@Argument id: Long): Int {
        bookStoreRepository.deleteById(id)
        // GraphQL requires return value,
        // but `deleteById` of spring data return nothing!
        // Is there better design?
        return 1
    }
}