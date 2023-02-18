package org.babyfish.jimmer.sql.example.graphql.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreTable;
import org.babyfish.jimmer.sql.example.graphql.entities.BookTableEx;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface BookStoreRepository extends JRepository<BookStore, Long> {

    BookStoreTable table = BookStoreTable.$;

    List<BookStore> findByNameLikeOrderByName(@Nullable String name);

    default List<Tuple2<Long, BigDecimal>> findIdAndAvgBookPrice(Collection<Long> ids) {
        return sql().createQuery(table)
                .where(table.id().in(ids))
                .groupBy(table.id())
                .select(
                        table.id(),
                        table.asTableEx().books(JoinType.LEFT).price().avg().coalesce(BigDecimal.ZERO)
                )
                .execute();
    }

    default List<Tuple2<Long, Long>> findIdAndNewestBookId(Collection<Long> ids) {
        BookTableEx book = BookTableEx.$;
        return sql().createQuery(table)
                .where(
                        Expression.tuple(
                                table.asTableEx().books().name(),
                                table.asTableEx().books().edition()
                        ).in(
                                sql().createSubQuery(book)
                                        .where(book.store().id().in(ids))
                                        .groupBy(book.name())
                                        .select(
                                                book.name(),
                                                book.edition().max()
                                        )
                        )
                )
                .select(
                        table.id(),
                        table.asTableEx().books().id()
                )
                .execute();
    }
}
