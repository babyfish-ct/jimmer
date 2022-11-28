package org.babyfish.jimmer.sql.example.graphql.controller;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.example.graphql.dal.AuthorRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.Author;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorProps;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.input.AuthorInput;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class AuthorController {

    private final JSqlClient sqlClient;

    private final AuthorRepository authorRepository;

    public AuthorController(
            JSqlClient sqlClient,
            AuthorRepository authorRepository
    ) {
        this.sqlClient = sqlClient;
        this.authorRepository = authorRepository;
    }

    // --- Query ---

    @QueryMapping
    public List<Author> authors(@Argument @Nullable String name) {
        return authorRepository.find(name);
    }

    // --- Association ---

    @BatchMapping
    public Map<Author, List<Book>> books(
            List<Author> authors
    ) {
        return sqlClient
                .getLoaders()
                .list(AuthorProps.BOOKS)
                .batchLoad(authors);
    }

    // --- Mutation ---

    @MutationMapping
    public Author saveAuthor(@Argument AuthorInput input) {
        return sqlClient.getEntities().save(input.toAuthor()).getModifiedEntity();
    }

    @MutationMapping
    public int deleteAuthor(@Argument long id) {
        return sqlClient
                .getEntities()
                .delete(Author.class, id)
                .getAffectedRowCount(AffectedTable.of(Author.class));
    }
}
