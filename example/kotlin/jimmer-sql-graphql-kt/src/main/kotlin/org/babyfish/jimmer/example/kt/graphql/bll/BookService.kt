package org.babyfish.jimmer.example.kt.graphql.bll

import org.babyfish.jimmer.example.kt.graphql.dal.BookRepository
import org.babyfish.jimmer.example.kt.graphql.entities.Author
import org.babyfish.jimmer.example.kt.graphql.entities.Book
import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.example.kt.graphql.entities.input.BookInput
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

@Controller
class BookService(
    private val bookRepository: BookRepository
) {

    // --- Query ---

    @QueryMapping
    fun books(
        @Argument name: String?,
        @Argument storeName: String?,
        @Argument authorName: String?
    ): List<Book> =
        bookRepository.find(name, storeName, authorName)

    // --- Association ---

    @BatchMapping
    fun store(
        // Must use `java.util.List` because Spring-GraphQL has a bug: #454
        books: java.util.List<Book>
    ): Map<Book, BookStore> =
        bookRepository.graphql.load(Book::store, books)

    @BatchMapping
    fun authors(
        // Must use `java.util.List` because Spring-GraphQL has a bug: #454
        books: java.util.List<Book>
    ): Map<Book, List<Author>> =
        bookRepository.graphql.load(Book::authors, books)

    // --- Mutation ---

    @MutationMapping
    @Transactional
    fun saveBook(@Argument input: BookInput): Book =
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