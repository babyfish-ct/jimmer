package org.babyfish.jimmer.sql.example.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.ThrowsAll;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.example.model.dto.BookSpecification;
import org.babyfish.jimmer.sql.example.repository.BookRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.dto.BookInput;
import org.babyfish.jimmer.sql.example.model.dto.CompositeBookInput;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.SaveErrorCode;
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
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/simpleList")
    public List<@FetchBy("SIMPLE_FETCHER") Book> findSimpleBooks() { // ❶
        return bookRepository.findAll(SIMPLE_FETCHER, BookProps.NAME, BookProps.EDITION.desc());
    }

    /**
     * The functionality of this method is the same as
     * {@link #findBooksBySuperQBE(int, int, String, BookSpecification)}
     */
    @GetMapping("/list")
    public Page<@FetchBy("DEFAULT_FETCHER") Book> findBooks( // ❷
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
    public @FetchBy("COMPLEX_FETCHER") Book findComplexBook( // ❸
            @PathVariable("id") long id
    ) {
        return bookRepository.findNullable(id, COMPLEX_FETCHER);
    }

    private static final Fetcher<Book> SIMPLE_FETCHER =
            BookFetcher.$.name().edition();

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

    private static final Fetcher<Book> COMPLEX_FETCHER =
            BookFetcher.$
                    .allScalarFields()
                    .tenant(false)
                    .store(
                            BookStoreFetcher.$
                                    .allScalarFields()
                                    .avgPrice()
                    )
                    .authors(
                            AuthorFetcher.$
                                    .allScalarFields()
                    );

    @PutMapping
    @ThrowsAll(SaveErrorCode.class) // ❹
    public Book saveBook(@RequestBody BookInput input) { // ❺
        return bookRepository.save(input);
    }

    @PutMapping("/composite")
    @ThrowsAll(SaveErrorCode.class) // ❻
    public Book saveCompositeBook(@RequestBody CompositeBookInput input) { // ❼
        return bookRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteBook(@PathVariable("id") long id) {
        bookRepository.deleteById(id);
    }
}

/*----------------Documentation Links----------------
❶ ❷ ❸ https://babyfish-ct.github.io/jimmer/docs/spring/client/api#declare-fetchby
❹ ❻ https://babyfish-ct.github.io/jimmer/docs/spring/client/error#allow-to-throw-all-exceptions-of-family
❺ ❼ https://babyfish-ct.github.io/jimmer/docs/mutation/save-command/input-dto/
---------------------------------------------------*/
