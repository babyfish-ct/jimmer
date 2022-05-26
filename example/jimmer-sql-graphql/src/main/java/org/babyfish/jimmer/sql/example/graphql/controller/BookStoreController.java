package org.babyfish.jimmer.sql.example.graphql.controller;

import jdk.javadoc.internal.doclets.formats.html.markup.Table;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.example.graphql.dal.BookRepository;
import org.babyfish.jimmer.sql.example.graphql.dal.BookStoreRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreTableEx;
import org.babyfish.jimmer.sql.example.graphql.input.BookStoreInput;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class BookStoreController {

    private SqlClient sqlClient;

    private final BookStoreRepository bookStoreRepository;

    private final BookRepository bookRepository;

    public BookStoreController(
            SqlClient sqlClient,
            BookStoreRepository bookStoreRepository,
            BookRepository bookRepository
    ) {
        this.sqlClient = sqlClient;
        this.bookStoreRepository = bookStoreRepository;
        this.bookRepository = bookRepository;
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
            List<BookStore> bookStores
    ) {
        return sqlClient.getListLoader(
                BookStoreTableEx.class,
                BookStoreTableEx::books,
                (q, book) -> q.orderBy(book.name())
        ).batchLoad(bookStores);
    }

    // --- Calculation ---

    @BatchMapping
    public Map<BookStore, BigDecimal> avgPrice(List<BookStore> stores) {
        Map<Long, BigDecimal> avgPriceMap =
                bookRepository.findAvgPricesByStoreIds(
                        stores.stream().map(BookStore::id).collect(Collectors.toList())
                );
        return stores.stream().collect(
                Collectors.toMap(
                        Function.identity(),
                        it -> {
                            BigDecimal avgPrice = avgPriceMap.get(it.id());
                            return avgPrice != null ? avgPrice : BigDecimal.ZERO;
                        }
                )
        );
    }

    // --- Mutation ---

    @MutationMapping
    @Transactional
    public BookStore saveBookStore(@Argument BookStoreInput input) {
        return sqlClient.getEntities().save(input.toBookStore()).getModifiedEntity();
    }

    @MutationMapping
    @Transactional
    public int deleteBookStore(Long id) {
        return sqlClient
                .getEntities()
                .delete(BookStore.class, id)
                .getAffectedRowCount(AffectedTable.of(BookStore.class));
    }
}
