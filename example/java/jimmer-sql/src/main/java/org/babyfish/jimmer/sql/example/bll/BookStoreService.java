package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.dal.BookRepository;
import org.babyfish.jimmer.sql.example.dal.BookStoreRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.input.BookStoreInput;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A real project should be a three-tier architecture consisting
 * of repository, service, and controller.
 *
 * This demo has no business logic, its purpose is only to tell users
 * how to use jimmer with the <b>least</b> code. Therefore, this demo
 * does not follow this convention, and let services be directly
 * decorated by `@RestController`, not `@Service`.
 */
@RestController
@RequestMapping("/bookStore")
@Transactional
public class BookStoreService {

    private final BookStoreRepository bookStoreRepository;

    public BookStoreService(BookStoreRepository bookStoreRepository) {
        this.bookStoreRepository = bookStoreRepository;
    }

    @GetMapping("/simpleList")
    public List<@FetchBy("SIMPLE_FETCHER") BookStore> findSimpleStores() {
        return bookStoreRepository.findAll(
                SIMPLE_FETCHER,
                BookStoreProps.NAME
        );
    }

    @GetMapping("/list")
    public List<@FetchBy("DEFAULT_FETCHER") BookStore> findStores() {
        return bookStoreRepository.findAll(
                DEFAULT_FETCHER,
                BookStoreProps.NAME
        );
    }

    @GetMapping("/{id}/withAllBooks")
    @Nullable
    public @FetchBy("WITH_ALL_BOOKS_FETCHER") BookStore findComplexStoreWithAllBooks(
            @PathVariable("id") long id
    ) {
        return bookStoreRepository.findNullable(id, WITH_ALL_BOOKS_FETCHER);
    }

    @GetMapping("/{id}/withNewestBooks")
    @Nullable
    public @FetchBy("WITH_NEWEST_BOOKS_FETCHER") BookStore findComplexStoreWithNewestBooks(
            @PathVariable("id") long id
    ) {
        return bookStoreRepository.findNullable(id, WITH_NEWEST_BOOKS_FETCHER);
    }

    private static final Fetcher<BookStore> SIMPLE_FETCHER =
            BookStoreFetcher.$.name();

    private static final Fetcher<BookStore> DEFAULT_FETCHER =
            BookStoreFetcher.$.allScalarFields();

    private static final Fetcher<BookStore> WITH_ALL_BOOKS_FETCHER =
            BookStoreFetcher.$
                    .allScalarFields()
                    .avgPrice()
                    .books(
                            BookFetcher.$
                                    .allScalarFields()
                                    .tenant(false)
                                    .authors(
                                            AuthorFetcher.$
                                                    .allScalarFields()
                                    )
                    );

    private static final Fetcher<BookStore> WITH_NEWEST_BOOKS_FETCHER =
            BookStoreFetcher.$
                    .allScalarFields()
                    .avgPrice()
                    .newestBooks(
                            BookFetcher.$
                                    .allScalarFields()
                                    .tenant(false)
                                    .authors(
                                            AuthorFetcher.$
                                                    .allScalarFields()
                                    )
                    );

    @PutMapping
    public BookStore saveBookStore(@RequestBody BookStoreInput input) {
        return bookStoreRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteBookStore(@PathVariable("id") long id) {
        bookStoreRepository.deleteById(id);
    }
}
