package org.babyfish.jimmer.sql.example.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.repository.BookStoreRepository;
import org.babyfish.jimmer.sql.example.service.dto.BookStoreInput;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Why add spring web annotations to the service class?
 *
 * The success and popularity of rich client technologies represented by React, Vue and Angular
 * have greatly reduced the significance of the Controller layer on the spring server side.
 *
 * Moreover, over-bloated code structures are not conducive to demonstrating the capabilities
 * of the framework with small examples. Therefore, this example project no longer adheres to
 * dogmatism and directly adds spring web annotations to the service class.
 */
@RestController
@RequestMapping("/bookStore")
@Transactional
public class BookStoreService implements Fetchers {

    private final BookStoreRepository bookStoreRepository;

    public BookStoreService(BookStoreRepository bookStoreRepository) {
        this.bookStoreRepository = bookStoreRepository;
    }

    @GetMapping("/simpleList")
    public List<@FetchBy("SIMPLE_FETCHER") BookStore> findSimpleStores() { // ❶
        return bookStoreRepository.findAll(
                SIMPLE_FETCHER,
                BookStoreProps.NAME
        );
    }

    @GetMapping("/list")
    public List<@FetchBy("DEFAULT_FETCHER") BookStore> findStores() { // ❷
        return bookStoreRepository.findAll(
                DEFAULT_FETCHER,
                BookStoreProps.NAME
        );
    }

    @GetMapping("/complexList")
    public List<@FetchBy("WITH_ALL_BOOKS_FETCHER") BookStore> findComplexStores() { // ❸
        return bookStoreRepository.findAll(
                WITH_ALL_BOOKS_FETCHER,
                BookStoreProps.NAME
        );
    }

    @GetMapping("/{id}/withAllBooks")
    @Nullable
    public @FetchBy("WITH_ALL_BOOKS_FETCHER") BookStore findComplexStoreWithAllBooks( // ❹
            @PathVariable("id") long id
    ) {
        return bookStoreRepository.findNullable(id, WITH_ALL_BOOKS_FETCHER);
    }

    @GetMapping("/{id}/withNewestBooks")
    @Nullable
    public @FetchBy("WITH_NEWEST_BOOKS_FETCHER") BookStore findComplexStoreWithNewestBooks( // ❺
            @PathVariable("id") long id
    ) {
        return bookStoreRepository.findNullable(id, WITH_NEWEST_BOOKS_FETCHER);
    }

    private static final Fetcher<BookStore> SIMPLE_FETCHER =
            BOOK_STORE_FETCHER.name();

    private static final Fetcher<BookStore> DEFAULT_FETCHER =
            BOOK_STORE_FETCHER.allScalarFields();

    private static final Fetcher<BookStore> WITH_ALL_BOOKS_FETCHER =
            BOOK_STORE_FETCHER
                    .allScalarFields()
                    .avgPrice()
                    .books(
                            BOOK_FETCHER
                                    .allScalarFields()
                                    .tenant(false)
                                    .authors(
                                            AUTHOR_FETCHER
                                                    .allScalarFields()
                                    )
                    );

    private static final Fetcher<BookStore> WITH_NEWEST_BOOKS_FETCHER =
            BOOK_STORE_FETCHER
                    .allScalarFields()
                    .avgPrice()
                    .newestBooks(
                            BOOK_FETCHER
                                    .allScalarFields()
                                    .tenant(false)
                                    .authors(
                                            AUTHOR_FETCHER
                                                    .allScalarFields()
                                    )
                    );

    @PutMapping
    public BookStore saveBookStore(@RequestBody BookStoreInput input) { // ❼
        return bookStoreRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteBookStore(@PathVariable("id") long id) {
        bookStoreRepository.deleteById(id);
    }
}

/*----------------Documentation Links----------------
❶ ❷ ❸ ❹ ❺ https://babyfish-ct.github.io/jimmer/docs/spring/client/api#declare-fetchby
❻ https://babyfish-ct.github.io/jimmer/docs/spring/client/error#allow-to-throw-all-exceptions-of-family
❼ https://babyfish-ct.github.io/jimmer/docs/mutation/save-command/input-dto/
---------------------------------------------------*/
