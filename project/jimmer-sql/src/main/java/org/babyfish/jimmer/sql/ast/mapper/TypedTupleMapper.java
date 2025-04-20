package org.babyfish.jimmer.sql.ast.mapper;

import org.babyfish.jimmer.sql.ast.Selection;

import java.util.List;

public interface TypedTupleMapper<T> {

    Class<T> getTupleType();

    int size();

    <S extends Selection<?>> S get(int index);

    List<Selection<?>> selections();
}
