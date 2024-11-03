package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;

public interface TypedRootQueryImplementor<R> extends TypedRootQuery<R>, TypedQueryImplementor {

    boolean isForUpdate();

    TypedRootQuery<R> forOne();
}
