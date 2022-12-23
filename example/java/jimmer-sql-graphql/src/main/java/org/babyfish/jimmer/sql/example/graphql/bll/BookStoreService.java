package org.babyfish.jimmer.sql.example.graphql.bll;

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
public class BookStoreService {

    private final BookStoreRepository bookStoreRepository;

    public BookStoreService(BookStoreRepository bookStoreRepository) {
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
        return bookStoreRepository.graphql().load(BookStoreProps.BOOKS, stores);
    }

    // --- Calculation ---

    @BatchMapping
    public Map<BookStore, BigDecimal> avgPrice(List<BookStore> stores) {
        return bookStoreRepository.graphql().load(BookStoreProps.AVG_PRICE, stores);
    }

    // --- Mutation ---

    @MutationMapping
    @Transactional
    public BookStore saveBookStore(@Argument BookStoreInput input) {
        return bookStoreRepository.save(input);
    }

    @MutationMapping
    @Transactional
    public int deleteBookStore(@Argument long id) {
        bookStoreRepository.deleteById(id);
        // GraphQL requires return value,
        // but `deleteById` of spring-data does not support value.
        // Is there a better design?
        return 1;
    }
}
