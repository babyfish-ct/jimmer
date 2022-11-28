package org.babyfish.jimmer.sql.example.graphql.controller;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.example.graphql.dal.BookStoreRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreProps;
import org.babyfish.jimmer.sql.example.graphql.entities.input.BookStoreInput;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class BookStoreController {

    private final JSqlClient sqlClient;

    private final BookStoreRepository bookStoreRepository;

    public BookStoreController(
            JSqlClient sqlClient,
            BookStoreRepository bookStoreRepository
    ) {
        this.sqlClient = sqlClient;
        this.bookStoreRepository = bookStoreRepository;
    }

    // --- Query ---

    @QueryMapping
    public List<BookStore> bookStores(
            @Argument @Nullable String name
    ) {
        return bookStoreRepository.find(name);
    }

    // --- Association ---

    @BatchMapping
    public Map<BookStore, List<Book>> books(
            List<BookStore> stores
    ) {
        return sqlClient
                .getLoaders()
                .list(BookStoreProps.BOOKS)
                .batchLoad(stores);
    }

    // --- Calculation ---

    @BatchMapping
    public Map<BookStore, BigDecimal> avgPrice(List<BookStore> stores) {
        return sqlClient
                .getLoaders()
                .value(BookStoreProps.AVG_PRICE)
                .batchLoad(stores);
    }

    // --- Mutation ---

    @MutationMapping
    @Transactional
    public BookStore saveBookStore(@Argument BookStoreInput input) {
        return sqlClient.getEntities().save(input.toBookStore()).getModifiedEntity();
    }

    @MutationMapping
    @Transactional
    public int deleteBookStore(@Argument long id) {
        return sqlClient
                .getEntities()
                .delete(BookStore.class, id)
                .getAffectedRowCount(AffectedTable.of(BookStore.class));
    }
}
