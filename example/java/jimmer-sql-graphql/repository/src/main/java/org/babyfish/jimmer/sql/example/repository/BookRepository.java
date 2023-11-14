package org.babyfish.jimmer.sql.example.repository;

import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.model.AuthorTableEx;
import org.babyfish.jimmer.sql.example.model.Book;
import org.babyfish.jimmer.sql.example.model.BookTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BookRepository extends JRepository<Book, Long> { // ❶

    BookTable table = BookTable.$;

    /**
     * Manually implement complex query.
     *
     * <p>The functionality of this method is the same as the super QBE method
     * {@link #find(Specification, Sort)}</p>
     */
    default List<Book> findBooks( // ❷
            @Nullable String name,
            @Nullable BigDecimal minPrice,
            @Nullable BigDecimal maxPrice,
            @Nullable String storeName,
            @Nullable String authorName,
            @Nullable String sortCode
    ) {
        AuthorTableEx author = AuthorTableEx.$;
        return sql()
                .createQuery(table)
                .whereIf( // ❹
                        name != null && !name.isEmpty(),
                        table.name().ilike(name)
                )
                .whereIf(minPrice != null, () -> table.price().ge(minPrice))
                .whereIf(maxPrice != null, () -> table.price().le(maxPrice))
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
                                .select(author.books().id()) // ❾
                        )
                )
                .orderBy(Order.makeOrders(table, sortCode != null ? sortCode : "name asc"))
                .select(table)
                .execute();
    }

    /**
     * Super QBE.
     *
     * <p>The functionality of this method is the same as the manual method
     * {@link #findBooks(String, BigDecimal, BigDecimal, String, String, String)}</p>
     */
    List<Book> find(Specification<Book> specification, Sort sort);

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
                                                // Apply root predicate to sub query is faster here.
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
