package org.babyfish.jimmer.example.kt.graphql.controller

import org.babyfish.jimmer.example.kt.graphql.dal.AuthorRepository
import org.babyfish.jimmer.example.kt.graphql.entities.Author
import org.babyfish.jimmer.example.kt.graphql.entities.Book
import org.babyfish.jimmer.example.kt.graphql.entities.input.AuthorInput
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class AuthorController(
    private val sqlClient: KSqlClient,
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
        sqlClient
            .loaders
            .list(Author::books)
            .batchLoad(authors)

    // --- Mutation ---

    @MutationMapping
    fun saveAuthor(@Argument input: AuthorInput): Author =
        sqlClient
            .entities
            .save(input.toAuthor())
            .modifiedEntity

    @MutationMapping
    fun deleteAuthor(@Argument id: Long): Int {
        return sqlClient
            .entities
            .delete(Author::class, id)
            .affectedRowCount(Author::class)
    }
}