package org.babyfish.jimmer.client.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.model.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

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

    @GetMapping("/tuples")
    Page<
            Tuple2<
                    ? extends @FetchBy("BOOK_FETCHER") Book,
                    ? extends @FetchBy("AUTHOR_FETCHER") Author
            >
    > findTuples(
            @RequestParam String name,
            @RequestParam int pageIndex,
            @RequestParam int pageSize
    );

    @PutMapping("/book")
    Book saveBooks(@RequestBody BookInput input);

    @DeleteMapping("/book/{id}")
    int deleteBook(@PathVariable("id") long id);
}
