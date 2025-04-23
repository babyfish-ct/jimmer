package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.mapper.TypedTupleMapper;

public class MapperSelectionImpl<T> implements MapperSelection<T> {

    private final TypedTupleMapper<T> mapper;

    public MapperSelectionImpl(TypedTupleMapper<T> mapper) {
        this.mapper = mapper;
    }

    @Override
    public TypedTupleMapper<T> getMapper() {
        return mapper;
    }
}
