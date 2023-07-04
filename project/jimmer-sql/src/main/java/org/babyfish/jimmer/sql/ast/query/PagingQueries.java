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
        if (pageSize == 0) {
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

        long longOffset = (long)pageIndex * pageSize;
        if (longOffset > Integer.MAX_VALUE - pageSize) {
            throw new IllegalArgumentException("offset is too big");
        }
        int total = query.count(con);
        if (longOffset >= total) {
            return pageFactory.create(
                    Collections.emptyList(),
                    0,
                    queryImplementor
            );
        }

        ConfigurableRootQuery<?, E> reversedQuery = null;
        if (longOffset + pageSize / 2 > total / 2) {
            reversedQuery = query.reverseSorting();
        }

        List<E> entities;
        if (reversedQuery != null) {
            int limit;
            int offset = (int)(total - longOffset - pageSize);
            if (offset < 0) {
                limit = pageSize + offset;
                offset = 0;
            } else {
                limit = pageSize;
            }
            entities = reversedQuery
                    .limit(limit, offset)
                    .execute(con);
            Collections.reverse(entities);
        } else {
            entities = query
                    .limit(pageSize, (int) longOffset)
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
                int totalCount,
                ConfigurableRootQueryImplementor<?, E> queryImplementor
        );
    }
}
