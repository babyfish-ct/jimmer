package org.babyfish.jimmer.sql.example.controller;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.example.model.input.BookInput;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookController {

    private final JSqlClient sqlClient;

    public BookController(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @GetMapping("/stores")
    public List<BookStore> stores(
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

    @GetMapping("/books")
    public Page<Book> books(
            @RequestParam(defaultValue = "false") boolean fetch,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String storeName,
            @RequestParam(defaultValue = "") String authorName,
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize
    ) {

        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;

        ConfigurableRootQuery<BookTable, Book> query = sqlClient
                .createQuery(book)
                .whereIf(
                        name != null && !name.isEmpty(),
                        book.name().ilike(name)
                )
                .whereIf(
                        storeName != null && !storeName.isEmpty(),
                        book.store().name().ilike(storeName)
                )
                .whereIf(
                        authorName != null && !authorName.isEmpty(),
                        book.id().in(sqlClient
                                .createSubQuery(author)
                                .where(
                                        Predicate.or(
                                                author.firstName().ilike(authorName),
                                                author.lastName().ilike(authorName)
                                        )
                                )
                                .select(author.books().id())
                        )
                )
                .select(
                        fetch ?
                                book.fetch(
                                        BookFetcher.$
                                                .allScalarFields()
                                                .store(
                                                        BookStoreFetcher.$
                                                                .allScalarFields()
                                                                .avgPrice()
                                                )
                                                .authors(
                                                        AuthorFetcher.$
                                                                .allScalarFields()
                                                )
                                ) :
                                book
                );

        int rowCount = query.count();
        int pageCount = (rowCount + pageSize - 1) / pageSize;
        List<Book> books = query
                .limit(pageSize, pageIndex * pageSize)
                .execute();
        return new Page<>(books, rowCount, pageCount);
    }

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
