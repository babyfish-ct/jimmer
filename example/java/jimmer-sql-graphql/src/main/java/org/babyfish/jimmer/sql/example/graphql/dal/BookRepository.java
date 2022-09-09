package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorTableEx;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookTable;
import org.babyfish.jimmer.sql.fluent.Fluent;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class BookRepository {

    private final JSqlClient sqlClient;

    public BookRepository(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public List<Book> find(
            @Nullable String name,
            @Nullable String storeName,
            @Nullable String authorName
    ) {
        Fluent fluent = sqlClient.createFluent();
        BookTable book = new BookTable();
        AuthorTableEx author = new AuthorTableEx();

        return fluent
                .query(book)
                .whereIf(
                        StringUtils.hasText(name),
                        () -> book.name().ilike(name, LikeMode.START)
                )
                .whereIf(
                        StringUtils.hasText(storeName),
                        () -> book.store().name().ilike(storeName, LikeMode.START)
                )
                .whereIf(
                        StringUtils.hasText(authorName),
                        () -> book.id().in(fluent
                                .subQuery(author)
                                .where(
                                        Predicate.or(
                                                author.firstName().ilike(authorName, LikeMode.START),
                                                author.lastName().ilike(authorName, LikeMode.START)
                                        )
                                )
                                .select(author.books().id())
                        )
                )
                .orderBy(
                        book.name().asc(),
                        book.edition().desc()
                )
                .select(book)
                .execute();
    }
}
