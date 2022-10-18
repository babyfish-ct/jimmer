package org.babyfish.jimmer.sql.example.controller;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.example.model.*;
import org.babyfish.jimmer.sql.fluent.Fluent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        if (fetch) {
            return sqlClient.getEntities().findAll(
                    BookStoreFetcher.$
                            .allScalarFields()
                            .avgPrice()
                            .books(
                                    BookFetcher.$
                                            .allScalarFields()
                                            .authors(
                                                    AuthorFetcher.$.allScalarFields()
                                            )
                            ),
                    BookStoreProps.NAME.asc()
            );
        }
        return sqlClient.getEntities().findAll(
                BookStore.class,
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

        Fluent fluent = sqlClient.createFluent();
        BookTable book = new BookTable();
        AuthorTableEx author = new AuthorTableEx();

        ConfigurableRootQuery<BookTable, Book> query = fluent
                .query(book)
                .whereIf(
                        name != null && !name.isEmpty(),
                        () -> book.name().ilike(name)
                )
                .whereIf(
                        storeName != null && !storeName.isEmpty(),
                        () -> book.store().name().ilike(storeName)
                )
                .whereIf(
                        authorName != null && !authorName.isEmpty(),
                        () -> book.id().in(fluent
                                .subQuery(author)
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
}
