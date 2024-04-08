package org.babyfish.jimmer.spring.java.bll;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.java.dal.BookRepository;
import org.babyfish.jimmer.spring.java.model.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/defaultBooks")
    public Page<Book> findDefaultBooks(
            @RequestParam int pageIndex,
            @RequestParam int pageSize,
            @RequestParam String name,
            @RequestParam String storeName,
            @RequestParam String authorName
    ) {
        return bookRepository.findBooks(
                pageIndex,
                pageSize,
                name,
                storeName,
                authorName,
                null
        );
    }

    /**
     * Find books with simple format
     * @param pageIndex Page index starts from 1
     * @param pageSize How many rows in on page
     * @param name Optional value to filter `name`
     * @param storeName Optional value to filter `store.name`
     * @param authorName Optional value to filter `authors.name`
     * @return The books with simple format
     */
    @GetMapping("/simpleBooks")
    public Page<@FetchBy("SIMPLE_FETCHER") Book> findSimpleBooks(
            @RequestParam int pageIndex,
            @RequestParam int pageSize,
            @RequestParam String name,
            @RequestParam String storeName,
            @RequestParam String authorName
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

    /**
     * Find books with complex format
     * @param pageIndex Page index starts from 1
     * @param pageSize How many rows in on page
     * @param name Optional value to filter `name`
     * @param storeName Optional value to filter `store.name`
     * @param authorName Optional value to filter `authors.name`
     * @return The books with complex format
     */
    @GetMapping("/complexBooks")
    public Page<@FetchBy("COMPLEX_FETCHER") Book> findComplexBooks(
            @RequestParam int pageIndex,
            @RequestParam int pageSize,
            @RequestParam String name,
            @RequestParam String storeName,
            @RequestParam String authorName
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

    private static final Fetcher<Book> SIMPLE_FETCHER =
            BookFetcher.$.name();

    private static final BookFetcher COMPLEX_FETCHER =
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
}
