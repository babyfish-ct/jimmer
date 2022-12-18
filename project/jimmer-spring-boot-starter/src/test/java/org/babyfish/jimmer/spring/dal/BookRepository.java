package org.babyfish.jimmer.spring.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.model.*;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BookRepository extends JRepository<Book, Long> {

    BookTable table = BookTable.$;

    default Page<Book> findBooks(
            int pageIndex,
            int pageSize,
            @Nullable String name,
            @Nullable String storeName,
            @Nullable String authorName,
            Fetcher<Book> fetcher
    ) {
        AuthorTableEx author = AuthorTableEx.$;
        return pager(
                pageIndex,
                pageSize
        ).execute(
                sql().createQuery(table)
                        .whereIf(name != null, table.name().ilike(name))
                        .whereIf(storeName != null, table.store().name().ilike(name))
                        .whereIf(
                                authorName != null,
                                table.id().in(
                                        sql().createSubQuery(author)
                                                .where(
                                                        Predicate.or(
                                                                author.firstName().ilike(authorName),
                                                                author.lastName().ilike(authorName)
                                                        )
                                                )
                                                .select(author.books().id())
                                )
                        )
                        .select(table.fetch(fetcher))
        );
    }
}
