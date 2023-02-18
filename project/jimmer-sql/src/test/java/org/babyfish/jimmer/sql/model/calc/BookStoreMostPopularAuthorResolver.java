package org.babyfish.jimmer.sql.model.calc;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.model.BookStoreTable;

import java.util.*;

public class BookStoreMostPopularAuthorResolver implements TransientResolver<UUID, UUID> {

    private static final BookStoreTable table = BookStoreTable.$;

    private final JSqlClient sqlClient;

    public BookStoreMostPopularAuthorResolver(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public Map<UUID, UUID> resolve(Collection<UUID> ids) {

        List<Tuple3<UUID, UUID, Long>> tuples = sqlClient
                .createQuery(table)
                .where(table.id().in(ids))
                .groupBy(
                        table.id(),
                        table.asTableEx().books().authors().id()
                )
                .orderBy(
                        table.asTableEx().books().authors().count().desc(),
                        table.asTableEx().books().authors().id().asc()
                )
                .select(
                        table.id(),
                        table.asTableEx().books().authors().id(),
                        table.asTableEx().books().count()
                )
                .execute(TransientResolver.currentConnection());
        Map<UUID, UUID> map = new HashMap<>();
        for (Tuple3<UUID, UUID, Long> tuple : tuples) {
            UUID storeId = tuple.get_1();
            UUID authorId = tuple.get_2();
            map.putIfAbsent(storeId, authorId);
        }
        return map;
    }
}
