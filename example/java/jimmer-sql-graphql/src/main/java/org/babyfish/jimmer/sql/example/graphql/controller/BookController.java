package org.babyfish.jimmer.sql.example.graphql.controller;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.example.graphql.dal.BookRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.*;
import org.babyfish.jimmer.sql.example.graphql.entities.input.BookInput;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Controller
public class BookController {

    private final JSqlClient sqlClient;

    private final BookRepository bookRepository;

    public BookController(
            JSqlClient sqlClient,
            BookRepository bookRepository
    ) {
        this.sqlClient = sqlClient;
        this.bookRepository = bookRepository;
    }

    // --- Query ---

    @QueryMapping
    public List<Book> books(
            @Argument @Nullable String name,
            @Argument @Nullable String storeName,
            @Argument @Nullable String authorName
    ) {
        return bookRepository.find(name, storeName, authorName);
    }

    // --- Association ---

    @BatchMapping
    public Map<Book, BookStore> store(Collection<Book> books) {
        return sqlClient
                .getLoaders()
                .reference(BookProps.STORE)
                .batchLoad(books);
    }

    @BatchMapping
    public Map<Book, List<Author>> authors(List<Book> books) {
        return sqlClient
                .getLoaders()
                .list(BookProps.AUTHORS)
                .batchLoad(books);
    }

    // --- Mutation ---

    @MutationMapping
    @Transactional
    public Book saveBook(@Argument BookInput input) {
        return sqlClient.getEntities().save(input.toBook()).getModifiedEntity();
    }

    @MutationMapping
    @Transactional
    public int deleteBook(@Argument long id) {
        return sqlClient
                .getEntities()
                .delete(Book.class, id)
                .getAffectedRowCount(AffectedTable.of(Book.class));
    }
}
