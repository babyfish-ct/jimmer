package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public interface RecursiveBaseQueryCreator<T extends TableLike<?>> {

    TypedBaseQuery<T> create(RecursiveRef<T> recursiveRef);
}
