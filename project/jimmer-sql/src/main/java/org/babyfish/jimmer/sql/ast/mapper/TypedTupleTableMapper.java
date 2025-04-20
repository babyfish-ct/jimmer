package org.babyfish.jimmer.sql.ast.mapper;

import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public interface TypedTupleTableMapper<T, TT extends TableLike<T>> extends TypedTupleMapper<T> {

    Class<TT> getTupleTableType();
}
