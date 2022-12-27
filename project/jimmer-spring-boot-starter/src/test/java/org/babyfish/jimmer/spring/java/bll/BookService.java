package org.babyfish.jimmer.spring.java.bll;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.java.dal.BookRepository;
import org.babyfish.jimmer.spring.java.model.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PutMapping("/book")
    public Book save(BookInput input) {
        return bookRepository.save(input);
    }

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
}
