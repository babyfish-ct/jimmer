package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;

class ManyToManyViewRecursionStrategy<E> implements RecursionStrategy<E> {

    private final Field field;

    private ManyToManyViewRecursionStrategy(Field field) {
        this.field = field;
    }

    static <E> ManyToManyViewRecursionStrategy<E> of(Field field) {
        if (field.getRecursionStrategy() == null) {
            return null;
        }
        return new ManyToManyViewRecursionStrategy<>(field);
    }

    @Override
    public boolean isRecursive(Args<E> args) {
        throw new UnsupportedOperationException();
    }

    public Field getField() {
        return field;
    }
}
