package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.mapper.TypedTupleMapper;

public interface MapperSelection<T> extends Selection<T> {
    TypedTupleMapper<T> getMapper();
}
