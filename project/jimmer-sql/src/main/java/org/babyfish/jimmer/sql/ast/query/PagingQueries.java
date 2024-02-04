package org.babyfish.jimmer.sql.ast.query;

import java.sql.Connection;

/**
 * @see ConfigurableRootQuery#fetchPage(int, int, PageFactory) 
 * @see ConfigurableRootQuery#fetchPage(int, int, Connection, PageFactory)  
 */
@Deprecated
public class PagingQueries {

    private PagingQueries() {}

    public static <E, P> P execute(
            ConfigurableRootQuery<?, E> query,
            int pageIndex,
            int pageSize,
            PageFactory<E, P> pageFactory
    ) {
        return query.fetchPage(pageIndex, pageSize, pageFactory);
    }

    @SuppressWarnings("unchecked")
    public static <E, P> P execute(
            ConfigurableRootQuery<?, E> query,
            int pageIndex,
            int pageSize,
            Connection con,
            PageFactory<E, P> pageFactory
    ) {
        return query.fetchPage(pageIndex, pageSize, con, pageFactory);
    }
}
