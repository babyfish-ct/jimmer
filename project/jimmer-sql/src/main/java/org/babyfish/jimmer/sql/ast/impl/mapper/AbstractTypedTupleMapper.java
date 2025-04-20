package org.babyfish.jimmer.sql.ast.impl.mapper;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.mapper.TypedTupleMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTypedTupleMapper<T> implements TypedTupleMapper<T> {

    private final Class<T> tupleType;

    private final List<Selection<?>> selections;

    protected AbstractTypedTupleMapper(Class<T> tupleType, Selection<?>[] selections) {
        this.tupleType = tupleType;
        List<Selection<?>> list = new ArrayList<>(selections.length);
        list.addAll(Arrays.asList(selections));
        this.selections = Collections.unmodifiableList(list);
    }

    @Override
    public Class<T> getTupleType() {
        return tupleType;
    }

    @Override
    public int size() {
        return selections.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends Selection<?>> S get(int index) {
        return (S) selections.get(index);
    }

    @Override
    public List<Selection<?>> selections() {
        return selections;
    }
}
