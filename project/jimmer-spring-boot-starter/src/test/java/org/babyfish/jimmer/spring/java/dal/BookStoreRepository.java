package org.babyfish.jimmer.spring.java.dal;

import org.babyfish.jimmer.spring.java.model.*;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookStoreRepository extends JRepository<BookStore, UUID> {

    BookStoreTable table = BookStoreTable.$;

    default List<Tuple2<UUID, UUID>> findIdAndNewestBookIds(Collection<UUID> ids) {
        BookTableEx book = BookTableEx.$;
        return sql()
                .createQuery(table)
                .where(
                        Expression.tuple(
                                table.asTableEx().books().name(),
                                table.asTableEx().books().edition()
                        ).in(
                                sql()
                                        .createSubQuery(book)
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
