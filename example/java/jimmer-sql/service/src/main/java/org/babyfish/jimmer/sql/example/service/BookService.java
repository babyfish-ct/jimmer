package org.babyfish.jimmer.sql.example.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.repository.BookRepository;
import org.babyfish.jimmer.sql.example.service.dto.BookInput;
import org.babyfish.jimmer.sql.example.service.dto.BookSpecification;
import org.babyfish.jimmer.sql.example.service.dto.CompositeBookInput;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
@RequestMapping("/book")
@Transactional
public class BookService implements Fetchers {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/simpleList")
    public List<@FetchBy("SIMPLE_FETCHER") Book> findSimpleBooks() {
        return bookRepository.findAll(SIMPLE_FETCHER, BookProps.NAME, BookProps.EDITION.desc());
    }

    /**
     * The functionality of this method is the same as
     * {@link #findBooksBySuperQBE(int, int, String, BookSpecification)}
     */
    @GetMapping("/list")
    public Page<@FetchBy("DEFAULT_FETCHER") Book> findBooks(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            // The `sortCode` also support implicit join, like `store.name asc`
            @RequestParam(defaultValue = "name asc, edition desc") String sortCode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String authorName
    ) {
        return bookRepository.findBooks(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                name,
                minPrice,
                maxPrice,
                storeName,
                authorName,
                DEFAULT_FETCHER
        );
    }

    /**
     * The functionality of this method is the same as
     * {@link #findBooks(int, int, String, String, BigDecimal, BigDecimal, String, String)}
     */
    @GetMapping("/list/bySuperQBE")
    public Page<@FetchBy("DEFAULT_FETCHER") Book> findBooksBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            // The `sortCode` also support implicit join, like `store.name asc`
            @RequestParam(defaultValue = "name asc, edition desc") String sortCode,
            BookSpecification specification
    ) {
        return bookRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                DEFAULT_FETCHER
        );
    }

    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("COMPLEX_FETCHER") Book findComplexBook(
            @PathVariable("id") long id
    ) {
        return bookRepository.findNullable(id, COMPLEX_FETCHER);
    }

    /**
     * Simple Book DTO that only contains `id` and `name`
     */
    private static final Fetcher<Book> SIMPLE_FETCHER =
            BookFetcher.$.name().edition();

    /**
     * Default Book DTO that contains
     * <ul>
     *     <li>All scalar properties except `tenant` of current `Book` entity</li>
     *     <li>`id` and `name` of the associated `BookStore` object provided by many-to-one association `store`</li>
     *     <li>`id`, `firstName` and `lastName` of the associated `Author` objects provided by many-to-many association `authors`</li>
     * </ul>
     */
    private static final Fetcher<Book> DEFAULT_FETCHER =
            BookFetcher.$
                    .allScalarFields()
                    .tenant(false)
                    .store(
                            BookStoreFetcher.$
                                    .name()
                    )
                    .authors(
                            AuthorFetcher.$
                                    .firstName()
                                    .lastName()
                    );

    /**
     * Complex Book DTO that contains
     * <ul>
     *     <li>All scalar properties except `tenant` of current `Book` entity</li>
     *     <li>`id`, `name` and the calculation property `avgPrice` of the associated `BookStore` object provided by many-to-one association `store`</li>
     *     <li>all scalar properties of the associated `Author` objects provided by many-to-many association `authors`</li>
     * </ul>
     */
    private static final Fetcher<Book> COMPLEX_FETCHER =
            BOOK_FETCHER
                    .allScalarFields()
                    .tenant(false)
                    .store(
                            BOOK_STORE_FETCHER
                                    .allScalarFields()
                                    .avgPrice()
                    )
                    .authors(
                            AUTHOR_FETCHER
                                    .allScalarFields()
                    );

    @PutMapping
    public Book saveBook(@RequestBody BookInput input) throws SaveException {
        return bookRepository.save(input);
    }

    @PutMapping("/composite")
    public Book saveCompositeBook(@RequestBody CompositeBookInput input) throws SaveException {
        return bookRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteBook(@PathVariable("id") long id) {
        bookRepository.deleteById(id);
    }
}
