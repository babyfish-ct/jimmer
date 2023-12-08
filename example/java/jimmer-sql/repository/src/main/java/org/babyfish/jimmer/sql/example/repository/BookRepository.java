package org.babyfish.jimmer.sql.example.repository;

import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.model.Book;
import org.babyfish.jimmer.sql.example.model.BookTable;
import org.babyfish.jimmer.sql.example.model.Tables;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BookRepository extends JRepository<Book, Long>, Tables {

    BookTable table = BOOK_TABLE;

    /**
     * Manually implement complex query.
     *
     * <p>The functionality of this method is the same as the super QBE method
     * {@link #find(Pageable, Specification, Fetcher)}</p>
     */
    default Page<Book> findBooks(
            Pageable pageable,
            @Nullable String name,
            @Nullable BigDecimal minPrice,
            @Nullable BigDecimal maxPrice,
            @Nullable String storeName,
            @Nullable String authorName,
            @Nullable Fetcher<Book> fetcher
    ) {
        return pager(pageable)
                .execute(
                        sql()
                                .createQuery(table)
                                .whereIf(
                                        name != null && !name.isEmpty(),
                                        table.name().ilike(name)
                                )
                                .whereIf(minPrice != null, () -> table.price().ge(minPrice))
                                .whereIf(maxPrice != null, () -> table.price().le(maxPrice))
                                .whereIf(
                                        storeName != null && !storeName.isEmpty(),
                                        table.store().name().ilike(storeName)
                                )
                                .whereIf(
                                        authorName != null && !authorName.isEmpty(),
                                        table.authors(author ->
                                                Predicate.or(
                                                        author.firstName().ilike(authorName),
                                                        author.lastName().ilike(authorName)
                                                )
                                        )
                                )
                                .orderBy(SpringOrders.toOrders(table, pageable.getSort()))
                                .select(table.fetch(fetcher))
                );
    }

    /**
     * Super QBE.
     *
     * <p>The functionality of this method is the same as the manual method
     * {@link #findBooks(Pageable, String, BigDecimal, BigDecimal, String, String, Fetcher)}</p>
     */
    Page<Book> find(
            Pageable pageable,
            Specification<Book> specification,
            @Nullable Fetcher<Book> fetcher
    );

    default Map<Long, BigDecimal> findAvgPriceGroupByStoreId(Collection<Long> storeIds) {
        return Tuple2.toMap(
                sql()
                        .createQuery(table)
                        .where(table.storeId().in(storeIds))
                        .groupBy(table.storeId())
                        .select(
                                table.storeId(),
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
                                                .where(table.storeId().in(storeIds))
                                                .groupBy(table.name())
                                                .select(
                                                        table.name(),
                                                        table.edition().max()
                                                )
                                )
                        )
                        .select(
                                table.storeId(),
                                table.id()
                        )
                        .execute()
        );
    }
}

