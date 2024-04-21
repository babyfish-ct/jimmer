package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.common.*;
import org.babyfish.jimmer.client.java.model.*;
import org.babyfish.jimmer.client.java.model.dto.BookInput;
import org.babyfish.jimmer.client.meta.Api;
import org.babyfish.jimmer.client.java.model.Author;
import org.babyfish.jimmer.client.java.model.Book;
import org.babyfish.jimmer.client.java.model.Page;
import org.babyfish.jimmer.client.runtime.Operation;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The book service
 */
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
            .store(BookStoreFetcher.$.name().level())
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

    /**
     * Find Simple DTOs
     *
     * <p>The simple DTO only supports `id`, `name` and `storeId`</p>
     *
     * @return A list of simple book DTOs
     */
    @Api
    @GetMapping("/books/simple")
    List<@FetchBy("SIMPLE_FETCHER") Book> findSimpleBooks();

    /**
     * Find Complex DTOs
     *
     * <p>The complex DTO only supports the scalar properties of book, and associations `store` and `authors`</p>
     *
     * @param name The book name
     * @param storeName The name of the associated book store
     * @param authorName The names of the associated authors
     * @param minPrice The min price of the book
     * @param maxPrice The max price of the book
     * @return A list of complex book DTOs
     */
    @Api
    @GetMapping("/books/complex")
    List<@FetchBy("COMPLEX_FETCHER") Book> findComplexBooks(
            @RequestParam("namePattern") String name,
            @RequestParam(value = "storeIds", required = false) @Nullable long[] storeIds,
            @RequestParam(value = "storeName", required = false) @Nullable String storeName,
            @RequestParam(value = "authorName", required = false) @Nullable String authorName,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice
    );

    /**
     * Find Complex DTOs
     *
     * <p>The complex DTO only supports the scalar properties of book, and associations `store` and `authors`</p>
     *
     * @return A list of complex book DTOs
     */
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
            @RequestParam(value = "name", required = false) @Nullable String name,
            @RequestParam(value = "pageIndex", defaultVale = "0") int pageIndex,
            @RequestParam(value = "pageSize", defaultVale = "10") int pageSize
    );

    /**
     * @return An optional complex book DTO
     */
    @Api
    @GetMapping("/book/{id}")
    Optional<@FetchBy("COMPLEX_FETCHER") Book> findBook(@PathVariable("id") long id);

    @Api
    @PostMapping("/book")
    Book saveBook(@RequestBody BookInput input) throws SaveException;

    @Api
    @PutMapping("/book")
    Book updateBook(@RequestBody BookInput input) throws SaveException;

    @Api
    @PatchMapping("/book")
    Book patchBook(@RequestBody BookInput input) throws SaveException;

    @Api
    @DeleteMapping("/book/{id}")
    int deleteBook(@PathVariable("id") long bookId);

    @Api
    @GetMapping("/version")
    int version(
            @RequestHeader("accessToken") String accessToken,
            @Nullable @RequestHeader(value = "resourcePath", required = false) String path
    );
}
