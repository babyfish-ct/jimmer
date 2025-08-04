package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;

public interface RecursiveBaseQueryCreator<T extends BaseTable> {

    TypedBaseQuery<T> create(RecursiveRef<T> recursiveRef);
}
