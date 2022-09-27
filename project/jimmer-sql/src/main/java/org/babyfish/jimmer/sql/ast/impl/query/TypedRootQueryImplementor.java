package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;

public interface TypedRootQueryImplementor<R> extends TypedRootQuery<R> {

    boolean isForUpdate();
}
