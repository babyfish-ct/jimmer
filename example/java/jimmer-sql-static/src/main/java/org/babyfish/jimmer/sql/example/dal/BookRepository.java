package org.babyfish.jimmer.sql.example.dal;

import org.babyfish.jimmer.Static;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.example.model.AuthorTableEx;
import org.babyfish.jimmer.sql.example.model.Book;
import org.babyfish.jimmer.sql.example.model.BookTable;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepository extends JRepository<Book, Long> {

    BookTable table = BookTable.$;

    default <S extends Static<Book>> Page<S> findBooks(
            Pageable pageable,
            @Nullable String name,
            @Nullable String storeName,
            @Nullable String authorName,
            Class<S> staticType
    ) {
        AuthorTableEx author = AuthorTableEx.$;
        return pager(pageable)
                .execute(
                        sql()
                                .createQuery(table)
                                .whereIf(
                                        name != null && !name.isEmpty(),
                                        table.name().ilike(name)
                                )
                                .whereIf(
                                        storeName != null && !storeName.isEmpty(),
                                        table.store().name().ilike(storeName)
                                )
                                .whereIf(
                                        authorName != null && !authorName.isEmpty(),
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
                                .orderBy(SpringOrders.toOrders(table, pageable.getSort()))
                                .select(table.fetch(staticType))
                );
    }
}
