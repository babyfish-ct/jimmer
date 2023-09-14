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

public interface BookRepository extends JRepository<Book, Long> { // ❶

    BookTable table = BookTable.$;

    default Page<Book> findBooks( // ❷
            Pageable pageable,
            @Nullable String name,
            @Nullable String storeName,
            @Nullable String authorName,
            @Nullable Fetcher<Book> fetcher
    ) {
        AuthorTableEx author = AuthorTableEx.$;
        return pager(pageable) // ❸
                .execute(
                        sql()
                                .createQuery(table)
                                .whereIf( // ❹
                                        name != null && !name.isEmpty(),
                                        table.name().ilike(name)
                                )
                                .whereIf( // ❺
                                        storeName != null && !storeName.isEmpty(),
                                        table.store().name().ilike(storeName) // ❻
                                )
                                .whereIf( // ❼
                                        authorName != null && !authorName.isEmpty(),
                                        table.id().in(sql()
                                                .createSubQuery(author) //  ❽
                                                .where(
                                                        Predicate.or(
                                                                author.firstName().ilike(authorName),
                                                                author.lastName().ilike(authorName)
                                                        )
                                                )
                                                .select(author.books().id())
                                        )
                                )
                                .orderBy(SpringOrders.toOrders(table, pageable.getSort())) // ❾
                                .select(table.fetch(fetcher)) // ❿
                );
    }

    default Map<Long, BigDecimal> findAvgPriceGroupByStoreId(Collection<Long> storeIds) {
        return Tuple2.toMap(
                sql()
                        .createQuery(table)
                        .where(table.store().id().in(storeIds)) // ⓫
                        .groupBy(table.store().id()) // ⓬
                        .select(
                                table.store().id(), // ⓭
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
                                        sql().createSubQuery(table) // ⓮
                                                // Apply root predicate to sub query is faster here.
                                                .where(table.store().id().in(storeIds)) // ⓯
                                                .groupBy(table.name())
                                                .select(
                                                        table.name(),
                                                        table.edition().max()
                                                )
                                )
                        )
                        .select(
                                table.store().id(), // ⓰
                                table.id()
                        )
                        .execute()
        );
    }
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/spring/repository/concept

❷ https://babyfish-ct.github.io/jimmer/docs/spring/repository/default

❸ https://babyfish-ct.github.io/jimmer/docs/spring/repository/default#pagination
  https://babyfish-ct.github.io/jimmer/docs/query/paging/

❹ ❺ ❼ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-where

❻ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-join/

❽ ⓫ ⓮ https://babyfish-ct.github.io/jimmer/docs/query/sub-query

❿ https://babyfish-ct.github.io/jimmer/docs/query/object-fetcher/

⓬ ⓭ ⓯ ⓰ https://babyfish-ct.github.io/jimmer/docs/query/dynamic-join/optimization#ghost-joins
---------------------------------------------------*/
