package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.dal.BookRepository;
import org.babyfish.jimmer.sql.example.dal.BookStoreRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.dto.BookStoreInput;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookStore")
@Transactional
public class BookStoreService {

    private final BookStoreRepository bookStoreRepository;

    private final BookRepository bookRepository;

    public BookStoreService(
            BookStoreRepository bookStoreRepository,
            BookRepository bookRepository
    ) {
        this.bookStoreRepository = bookStoreRepository;
        this.bookRepository = bookRepository;
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

    @GetMapping("/complexList")
    public List<@FetchBy("COMPLEX_FETCHER") BookStore> findComplexStores() {
        return bookStoreRepository.findAll(
                COMPLEX_FETCHER,
                BookStoreProps.NAME
        );
    }

    private static final Fetcher<BookStore> SIMPLE_FETCHER =
            BookStoreFetcher.$.name();

    private static final Fetcher<BookStore> DEFAULT_FETCHER =
            BookStoreFetcher.$.allScalarFields();

    private static final Fetcher<BookStore> COMPLEX_FETCHER =
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

    @PutMapping
    public BookStore saveBookStore(@RequestBody BookStoreInput input) {
        return bookStoreRepository.save(input);
    }

    @DeleteMapping("/{id}")
    public void deleteBookStore(@PathVariable("id") long id) {
        bookStoreRepository.deleteById(id);
    }
}
