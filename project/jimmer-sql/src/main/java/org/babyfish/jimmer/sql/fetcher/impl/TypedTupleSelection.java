package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.ast.Selection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface TypedTupleSelection<T> extends Selection<T> {

    Class<T> getTupleType();

    Selection<?> get(int index);

    int size();

    static <T> Selection<T> of(Class<T> type, Selection<?> ... selections) {
        List<Selection<?>> list = Collections.unmodifiableList(
                Arrays.asList(selections)
        );
        return new TypedTupleSelection<T>() {

            @Override
            public Class<T> getTupleType() {
                return type;
            }

            @Override
            public Selection<?> get(int index) {
                return list.get(index);
            }

            @Override
            public int size() {
                return list.size();
            }

            @Override
            public String toString() {
                return "TypedTupleSelection{" +
                        "type=" + type.getName() +
                        ", selections=" + list +
                        '}';
            }
        };
    }
}
