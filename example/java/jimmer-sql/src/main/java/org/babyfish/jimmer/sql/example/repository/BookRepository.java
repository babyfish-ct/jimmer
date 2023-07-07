package org.babyfish.jimmer.sql.example.repository;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.model.AuthorTableEx;
import org.babyfish.jimmer.sql.example.model.Book;
import org.babyfish.jimmer.sql.example.model.BookTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BookRepository extends JRepository<Book, Long> {

    BookTable table = BookTable.$;

    default Page<Book> findBooks(
            Pageable pageable,
            @Nullable String name,
            @Nullable String storeName,
            @Nullable String authorName,
            @Nullable Fetcher<Book> fetcher
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
                                .select(table.fetch(fetcher))
                );
    }

    default Map<Long, BigDecimal> findAvgPriceGroupByStoreId(Collection<Long> storeIds) {
        return Tuple2.toMap(
                sql()
                        .createQuery(table)
                        .where(table.store().id().in(storeIds))
                        .groupBy(table.store().id())
                        .select(
                                table.store().id(),
                                table.price().avg()
                        )
                        .execute()
        );
    }

    default Map<Long, List<Long>> findNewestIdsGroupByStoreId(Collection<Long> storeIds) {
        return Tuple2.toMultiMap(
                sql()
                        .createQuery(table)
                        .where(
                                Expression.tuple(table.name(), table.edition()).in(
                                        sql().createSubQuery(table)
                                                // Apply `filter` for sub query is better.
                                                .where(table.store().id().in(storeIds))
                                                .groupBy(table.name())
                                                .select(
                                                        table.name(),
                                                        table.edition().max()
                                                )
                                )
                        )
                        .select(
                                table.store().id(),
                                table.id()
                        )
                        .execute()
        );
    }
}
