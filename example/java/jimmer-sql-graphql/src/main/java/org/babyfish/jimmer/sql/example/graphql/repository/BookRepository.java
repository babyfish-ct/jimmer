package org.babyfish.jimmer.sql.example.graphql.repository;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.graphql.entities.AuthorTableEx;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookTable;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
