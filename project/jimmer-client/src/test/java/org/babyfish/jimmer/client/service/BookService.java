package org.babyfish.jimmer.client.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.model.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

public interface BookService {

    Fetcher<Book> BOOK_FETCHER = BookFetcher.$
            .allScalarFields()
            .store(BookStoreFetcher.$.name())
            .authors(AuthorFetcher.$.firstName());

    Fetcher<Author> AUTHOR_FETCHER = AuthorFetcher.$
            .allScalarFields()
            .books(
                    BookFetcher.$
                            .name()
                            .store(BookStoreFetcher.$.name())
            );

    @GetMapping("/books")
    List<@FetchBy("BOOK_FETCHER") Book> findBooks(
            @RequestParam("name") String name,
            @RequestParam("storeName") @Nullable String storeName,
            @RequestParam("authorName") @Nullable String authorName,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice
    );

    @GetMapping("/tuples")
    Page<
            Tuple2<
                    ? extends @FetchBy("BOOK_FETCHER") Book,
                    ? extends @FetchBy("AUTHOR_FETCHER") Author
            >
    > findTuples(
            @RequestParam("name") @Nullable String name,
            @RequestParam("pageIndex") int pageIndex,
            @RequestParam("pageSize") int pageSize
    );

    @PutMapping("/book")
    Book saveBooks(@RequestBody BookInput input);

    @DeleteMapping("/book/{id}")
    int deleteBook(@PathVariable("id") long id);
}
