package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.ast.impl.query.PageSource;

import java.util.List;

@FunctionalInterface
public interface PageFactory<E, P> {

    P create(
            List<E> entities,
            long totalCount,
            PageSource source
    );

    static <E> PageFactory<E, Page<E>> standard() {
        return (rows, totalCount, source) -> {
            int limit = source.getPageSize();
            int totalPageCount = limit == Integer.MAX_VALUE ? 1 : (int)((totalCount + limit - 1) / limit);
            return new Page<>(
                    rows,
                    totalCount,
                    totalPageCount
            );
        };
    }
}
