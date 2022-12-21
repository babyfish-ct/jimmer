package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorTableEx;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookTable;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

public interface BookRepository extends JRepository<Book, Long> {

    BookTable table = BookTable.$;

    default List<Book> find(
            @Nullable String name,
            @Nullable String storeName,
            @Nullable String authorName
    ) {
        AuthorTableEx author = AuthorTableEx.$;

        return sql()
                .createQuery(table)
                .whereIf(
                        StringUtils.hasText(name),
                        table.name().ilike(name)
                )
                .whereIf(
                        StringUtils.hasText(storeName),
                        table.store().name().ilike(storeName)
                )
                .whereIf(
                        StringUtils.hasText(authorName),
                        table.id().in(sql()
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
                .orderBy(
                        table.name().asc(),
                        table.edition().desc()
                )
                .select(table)
                .execute();
    }
}
