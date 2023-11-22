package org.babyfish.jimmer.sql.example.kt.service

import org.babyfish.jimmer.spring.model.SortUtils
import org.babyfish.jimmer.sql.example.kt.model.Book
import org.babyfish.jimmer.sql.example.kt.repository.BookRepository
import org.babyfish.jimmer.sql.example.kt.service.dto.BookInput
import org.babyfish.jimmer.sql.example.kt.service.dto.BookSpecification
import org.babyfish.jimmer.sql.example.kt.service.dto.CompositeBookInput
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

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
class BookService(
    private val bookRepository: BookRepository
) {

    // --- Query ---

    @QueryMapping
    fun books(
        @Argument name: String?,
        @Argument minPrice: BigDecimal?,
        @Argument maxPrice: BigDecimal?,
        @Argument storeName: String?,
        @Argument authorName: String?,
        @Argument sortCode: String?
    ): List<Book> =
        bookRepository.findBooks(
            name = name,
            minPrice = minPrice,
            maxPrice = maxPrice,
            storeName = storeName,
            authorName = authorName,
            sortCode = sortCode
        )

    @QueryMapping
    fun booksBySuperQBE(
        @Argument specification: BookSpecification?,
        @Argument sortCode: String?
    ): List<Book> =
        bookRepository.find(
            specification ?: BookSpecification(),
            SortUtils.toSort(sortCode ?: "name asc")
        )

    // --- Mutation ---

    @MutationMapping
    @Transactional
    fun saveBook(@Argument input: BookInput): Book =
        bookRepository.save(input)

    @MutationMapping
    @Transactional
    fun saveCompositeBook(@Argument input: CompositeBookInput): Book =
        bookRepository.save(input)

    @MutationMapping
    @Transactional
    fun deleteBook(@Argument id: Long): Int {
        bookRepository.deleteById(id)
        // GraphQL requires return value,
        // but `deleteById` of spring data return nothing!
        // Is there better design?
        return 1
    }
}