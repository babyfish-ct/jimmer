package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableRootQueryImplementor;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

public class PagingQueries {

    private PagingQueries() {}

    public static <E, P> P execute(
            ConfigurableRootQuery<?, E> query,
            int pageIndex,
            int pageSize,
            PageFactory<E, P> pageFactory
    ) {
        return execute(query, pageIndex, pageSize, null, pageFactory);
    }

    @SuppressWarnings("unchecked")
    public static <E, P> P execute(
            ConfigurableRootQuery<?, E> query,
            int pageIndex,
            int pageSize,
            Connection con,
            PageFactory<E, P> pageFactory
    ) {
        ConfigurableRootQueryImplementor<?, E> queryImplementor =
                (ConfigurableRootQueryImplementor<?, E>) query;
        if (pageSize == 0 || pageSize == -1 || pageSize == Integer.MAX_VALUE) {
            List<E> entities = query.execute(con);
            return pageFactory.create(
                    entities,
                    entities.size(),
                    queryImplementor
            );
        }
        if (pageIndex < 0) {
            return pageFactory.create(
                    Collections.emptyList(),
                    0,
                    queryImplementor
            );
        }

        long offset = (long)pageIndex * pageSize;
        if (offset > Long.MAX_VALUE - pageSize) {
            throw new IllegalArgumentException("offset is too big");
        }
        long total = query.count(con);
        if (offset >= total) {
            return pageFactory.create(
                    Collections.emptyList(),
                    total,
                    queryImplementor
            );
        }

        ConfigurableRootQuery<?, E> reversedQuery = null;
        if (offset + pageSize / 2 > total / 2) {
            reversedQuery = query.reverseSorting();
        }

        List<E> entities;
        if (reversedQuery != null) {
            int limit;
            long reversedOffset = (int)(total - offset - pageSize);
            if (reversedOffset < 0) {
                limit = pageSize + (int)reversedOffset;
                reversedOffset = 0;
            } else {
                limit = pageSize;
            }
            entities = reversedQuery
                    .limit(limit, reversedOffset)
                    .execute(con);
            Collections.reverse(entities);
        } else {
            entities = query
                    .limit(pageSize, offset)
                    .execute(con);
        }
        return pageFactory.create(
                entities,
                total,
                queryImplementor
        );
    }

    @FunctionalInterface
    public interface PageFactory<E, P> {
        P create(
                List<E> entities,
                long totalCount,
                ConfigurableRootQueryImplementor<?, E> queryImplementor
        );
    }
}
