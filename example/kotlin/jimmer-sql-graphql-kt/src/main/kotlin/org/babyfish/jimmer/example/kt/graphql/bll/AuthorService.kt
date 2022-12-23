package org.babyfish.jimmer.example.kt.graphql.bll

import org.babyfish.jimmer.example.kt.graphql.dal.AuthorRepository
import org.babyfish.jimmer.example.kt.graphql.entities.Author
import org.babyfish.jimmer.example.kt.graphql.entities.Book
import org.babyfish.jimmer.example.kt.graphql.entities.input.AuthorInput
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class AuthorService(
    private val authorRepository: AuthorRepository
) {

    // --- Query ---

    @QueryMapping
    fun authors(
        @Argument name: String?
    ): List<Author?> =
        authorRepository.find(name)

    // --- Association ---

    @BatchMapping
    fun books(
        // Must use `java.util.List` because Spring-GraphQL has a bug: #454
        authors: java.util.List<Author>
    ): Map<Author, List<Book>> =
        authorRepository.graphql.load(Author::books, authors)

    // --- Mutation ---

    @MutationMapping
    fun saveAuthor(@Argument input: AuthorInput): Author =
        authorRepository.save(input)

    @MutationMapping
    fun deleteAuthor(@Argument id: Long): Int {
        authorRepository.deleteById(id)
        // GraphQL requires return value,
        // but `deleteById` of spring data return nothing!
        // Is there better design?
        return 1
    }
}