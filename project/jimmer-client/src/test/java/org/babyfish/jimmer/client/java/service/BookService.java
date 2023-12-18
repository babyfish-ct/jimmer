package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.java.model.*;
import org.babyfish.jimmer.client.meta.Api;
import org.babyfish.jimmer.client.runtime.Operation;
import org.babyfish.jimmer.client.meta.common.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Api("bookService")
@RequestMapping(value = "/java", method = Operation.HttpMethod.GET)
public interface BookService {

    /**
     * Simple Book DTO
     */
    Fetcher<Book> SIMPLE_FETCHER = BookFetcher.$.name().storeId();

    /**
     * Complex Book DTO
     */
    Fetcher<Book> COMPLEX_FETCHER = BookFetcher.$
            .allScalarFields()
            .store(BookStoreFetcher.$.name())
            .authors(
                    AuthorFetcher.$.allScalarFields().gender(false)
            );

    /**
     * Author DTO used to be a part of return value of `BookService.findTuples`
     */
    Fetcher<Author> AUTHOR_FETCHER = AuthorFetcher.$
            .allScalarFields()
            .books(
                    BookFetcher.$
                            .name()
                            .store(BookStoreFetcher.$.name())
            );

    @Api
    @GetMapping("/books/simple")
    List<@FetchBy("SIMPLE_FETCHER") Book> findSimpleBooks();

    @Api
    @GetMapping("/books/complex")
    List<@FetchBy("COMPLEX_FETCHER") Book> findComplexBooks(
            @RequestParam("name") String name,
            @RequestParam("storeName") @Nullable String storeName,
            @RequestParam("authorName") @Nullable String authorName,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice
    );

    @Api
    @GetMapping("/books/complex2")
    List<@FetchBy("COMPLEX_FETCHER") Book> findComplexBooksByArguments(
            FindBookArguments arguments
    );

    @Api
    @GetMapping("/tuples")
    Page<
            Tuple2<
                    ? extends @FetchBy("COMPLEX_FETCHER") Book,
                    ? extends @FetchBy(value = "AUTHOR_FETCHER", nullable = true) Author
            >
    > findTuples(
            @RequestParam("name") @Nullable String name,
            @RequestParam("pageIndex") int pageIndex,
            @RequestParam("pageSize") int pageSize
    );

    @Api
    @GetMapping("/book/{id}")
    Optional<@FetchBy("COMPLEX_FETCHER") Book> findBook(@PathVariable("id") long id);

    @Api
    @PutMapping("/book")
    Book saveBooks(@RequestBody BookInput input) throws SaveException;

    @Api
    @PatchMapping("/book")
    Book updateBooks(@RequestBody BookInput input) throws SaveException;

    @Api
    @DeleteMapping("/book/{id}")
    int deleteBook(@PathVariable("id") long id);

    @Api
    @RequestMapping("version")
    int version();
}
