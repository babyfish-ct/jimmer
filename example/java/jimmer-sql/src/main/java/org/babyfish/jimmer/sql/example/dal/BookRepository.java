package org.babyfish.jimmer.sql.example.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.model.AuthorTableEx;
import org.babyfish.jimmer.sql.example.model.Book;
import org.babyfish.jimmer.sql.example.model.BookProps;
import org.babyfish.jimmer.sql.example.model.BookTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BookRepository extends JRepository<Book, Long> {

    BookTable table = BookTable.$;

    default Page<Book> findBooks(
            int pageIndex,
            int pageSize,
            @Nullable String name,
            @Nullable String storeName,
            @Nullable String authorName,
            @Nullable Fetcher<Book> fetcher
    ) {
        AuthorTableEx author = AuthorTableEx.$;
        return pager(pageIndex, pageSize)
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
                                .orderBy(table.name().asc())
                                .select(table.fetch(fetcher))
                );
    }

    default Map<Long, Long> findNewestBookIdsByStoreIds(Collection<Long> storeIds) {
        List<Tuple2<Long, Long>> tuples = sql()
                .createQuery(table)
                .where(table.store().id().in(storeIds))
                .groupBy(table.store().id())
                .select(
                        table.store().id(),
                        table.id().max()
                )
                .execute();
        return Tuple2.toMap(tuples);
    }
}
