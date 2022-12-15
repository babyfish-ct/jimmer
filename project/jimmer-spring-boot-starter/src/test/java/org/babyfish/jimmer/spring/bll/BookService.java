package org.babyfish.jimmer.spring.bll;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.dal.BookRepository;
import org.babyfish.jimmer.spring.model.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

public class BookService {

    private static final Fetcher<Book> SIMPLE_FETCHER =
            BookFetcher.$.name();

    private static final Fetcher<Book> COMPLEX_FETCHER =
            BookFetcher.$
                    .allScalarFields()
                    .store(
                            BookStoreFetcher.$.name()
                    )
                    .authors(
                            AuthorFetcher.$
                                    .allScalarFields()
                                    .gender(false)
                    );

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<@FetchBy("SIMPLE_FETCHER") Book> findSimpleBooks(
            int pageIndex,
            int pageSize,
            String name,
            String storeName,
            String authorName
    ) {
        return bookRepository.findBooks(
                pageIndex,
                pageSize,
                name,
                storeName,
                authorName,
                SIMPLE_FETCHER
        );
    }

    public Page<@FetchBy("COMPLEX_FETCHER") Book> findComplexBooks(
            int pageIndex,
            int pageSize,
            String name,
            String storeName,
            String authorName
    ) {
        return bookRepository.findBooks(
                pageIndex,
                pageSize,
                name,
                storeName,
                authorName,
                COMPLEX_FETCHER
        );
    }
}
