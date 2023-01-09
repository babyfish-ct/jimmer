package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.dal.BookRepository;
import org.babyfish.jimmer.sql.example.dal.BookStoreRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
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

    @GetMapping("/stores/simple")
    public List<@FetchBy("SIMPLE_FETCHER") BookStore> findSimpleStores() {
        return bookStoreRepository.findAll(
                SIMPLE_FETCHER,
                BookStoreProps.NAME
        );
    }

    @GetMapping("/stores/complex")
    public List<@FetchBy("COMPLEX_FETCHER") BookStore> findComplexStores() {
        return bookStoreRepository.findAll(
                COMPLEX_FETCHER,
                BookStoreProps.NAME
        );
    }

    /*
     * In some cases, simple combination of different data have to be returned,
     * which cannot be achieved based on associated properties.
     *
     * For example, here we need each bookstore and its newest book,
     * not all its books: `BookStore.books`.
     *
     * This is a WEAK association at the business level.
     *
     * You can use ordinary object to assemble different entity objects
     * that are not strongly associated.
     *
     * You can use multiple `@FetchBy` annotations even if your return type
     * has multiple generic parameters, or even nested generic types.
     * This is why `@FetchBy` decorates generic parameters but not return types.
     */
    @GetMapping("stores/withNewestBook")
    public List<
            Tuple2<
                    @FetchBy("SIMPLE_FETCHER") BookStore,
                    @FetchBy(value = "NEWEST_BOOK_FETCHER", nullable = true) Book
            >
    > findStoresWithNewestBook() {

        List<BookStore> stores = bookStoreRepository.findAll(SIMPLE_FETCHER);

        // BookStoreId -> BookId
        Map<Long, Long> newestBookIdMap = stores.isEmpty() ?
                Collections.emptyMap() :
                bookRepository.findNewestBookIdsByStoreIds(
                        stores.stream().map(BookStore::id).collect(Collectors.toList())
                );

        // BookId -> Book
        Map<Long, Book> bookMap = newestBookIdMap.isEmpty() ?
                Collections.emptyMap() :
                bookRepository.findMapByIds(
                        newestBookIdMap.values(),
                        NEWEST_BOOK_FETCHER
                );

        return stores
                .stream()
                .map(store -> {
                    Long newestBookId = newestBookIdMap.get(store.id());
                    Book newestBook = bookMap.get(newestBookId);
                    return new Tuple2<>(store, newestBook);
                })
                .collect(Collectors.toList());
    }

    private static final Fetcher<BookStore> SIMPLE_FETCHER =
            BookStoreFetcher.$.name();

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

    private static final Fetcher<Book> NEWEST_BOOK_FETCHER =
            BookFetcher.$
                    .allScalarFields()
                    .tenant(false)
                    .authors(
                            AuthorFetcher.$
                                    .allScalarFields()
                    );
}
