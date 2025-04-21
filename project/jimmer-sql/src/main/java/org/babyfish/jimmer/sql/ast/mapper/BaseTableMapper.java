package org.babyfish.jimmer.sql.ast.mapper;

import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public interface BaseTableMapper<T, B extends TableLike<T>> extends TypedTupleMapper<T> {

    Class<B> getBaseTableType();
}
