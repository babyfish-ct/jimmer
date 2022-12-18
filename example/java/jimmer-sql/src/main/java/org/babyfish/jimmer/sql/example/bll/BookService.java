package org.babyfish.jimmer.sql.example.bll;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.example.dal.BookRepository;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.input.BookInput;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookService {

    private final JSqlClient sqlClient;

    private final BookRepository bookRepository;

    public BookService(JSqlClient sqlClient, BookRepository bookRepository) {
        this.sqlClient = sqlClient;
        this.bookRepository = bookRepository;
    }

    @GetMapping("/stores")
    public List<@FetchBy("SIMPLE_BOOK_FETCHER") BookStore> stores(
            @RequestParam(defaultValue = "false") boolean fetch
    ) {
        BookStoreFetcher fetcher = BookStoreFetcher.$.allScalarFields();
        if (fetch) {
            fetcher = fetcher
                    .avgPrice()
                    .books(
                            BookFetcher.$
                                    .allScalarFields()
                                    .authors(
                                            AuthorFetcher.$
                                                    .allScalarFields()
                                    )
                    );
        }
        return sqlClient.getEntities().findAll(
                fetcher,
                BookStoreProps.NAME.asc()
        );
    }

    @GetMapping("/books/simple")
    public Page<@FetchBy("COMPLEX_BOOK_FETCHER") Book> findSimpleBooks(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String storeName,
            @RequestParam(defaultValue = "") String authorName
    ) {
        return bookRepository.findBooks(
                pageIndex,
                pageSize,
                name,
                storeName,
                authorName,
                SIMPLE_BOOK_FETCHER
        );
    }

    @GetMapping("/books/complex")
    public Page<Book> findComplexBooks(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String storeName,
            @RequestParam(defaultValue = "") String authorName
    ) {
        return bookRepository.findBooks(
                pageIndex,
                pageSize,
                name,
                storeName,
                authorName,
                COMPLEX_BOOK_FETCHER
        );
    }

    private static final Fetcher<Book> SIMPLE_BOOK_FETCHER =
            BookFetcher.$.name();

    private static final Fetcher<Book> COMPLEX_BOOK_FETCHER =
            BookFetcher.$
                    .allScalarFields()
                    .store(
                            BookStoreFetcher.$
                                    .allScalarFields()
                    )
                    .authors(
                            AuthorFetcher.$
                                    .allScalarFields()
                                    .gender(false)
                    );

    @GetMapping("/authors")
    public List<Author> authors(
            @RequestParam(defaultValue = "false") boolean fetch,
            @RequestParam(defaultValue = "") String firstName,
            @RequestParam(defaultValue = "") String lastName,
            @RequestParam(required = false) Gender gender
    ) {
        Author author = AuthorDraft.$.produce(draft -> {
            if (!firstName.isEmpty()) {
                draft.setFirstName(firstName);
            }
            if (!lastName.isEmpty()) {
                draft.setLastName(lastName);
            }
            if (gender != null) {
                draft.setGender(gender);
            }
        });
        AuthorFetcher fetcher = null;
        if (fetch) {
            fetcher = AuthorFetcher.$
                    .allScalarFields()
                    .books(
                            BookFetcher.$
                                    .allScalarFields()
                                    .store(
                                            BookStoreFetcher.$
                                                    .allScalarFields()
                                                    .avgPrice()
                                    )
                    );
        }
        return sqlClient.getEntities().findByExample(
                Example.of(author)
                        .ilike(AuthorProps.FIRST_NAME)
                        .ilike(AuthorProps.LAST_NAME),
                fetcher
        );
    }

    @Transactional
    @PutMapping("/book")
    public Book saveBook(@RequestBody BookInput input) {
        return sqlClient.getEntities().save(input.toBook()).getModifiedEntity();
    }

    @Transactional
    @PutMapping("/book/dynamic")
    public Book saveBook(@RequestBody Book book) {
        return sqlClient
                .getEntities()
                .saveCommand(book)
                .configure(cfg -> cfg.setAutoAttachingAll())
                .execute()
                .getModifiedEntity();
    }
}
