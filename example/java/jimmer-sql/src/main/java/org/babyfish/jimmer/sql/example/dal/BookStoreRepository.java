package org.babyfish.jimmer.sql.example.dal;

import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.example.model.BookStore;
import org.babyfish.jimmer.sql.example.model.BookStoreTable;
import org.babyfish.jimmer.sql.example.model.BookTableEx;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface BookStoreRepository extends JRepository<BookStore, Long> {

    BookStoreTable table = BookStoreTable.$;

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
                        table.asTableEx().id()
                )
                .execute();
    }
}
