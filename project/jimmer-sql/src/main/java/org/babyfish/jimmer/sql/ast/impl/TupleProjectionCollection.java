package org.babyfish.jimmer.sql.ast.impl;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

class TupleProjectionCollection extends AbstractCollection<Object> {

    private final Collection<? extends TupleImplementor> tuples;

    private final int index;

    public TupleProjectionCollection(Collection<? extends TupleImplementor> tuples, int index) {
        this.tuples = tuples;
        this.index = index;
    }

    @Override
    public int size() {
        return tuples.size();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator<Object> iterator() {
        return new Itr(tuples.iterator(), index);
    }

    private static class Itr implements Iterator<Object> {

        private final Iterator<? extends TupleImplementor> baseItr;

        private final int index;

        private Itr(Iterator<? extends TupleImplementor> baseItr, int index) {
            this.baseItr = baseItr;
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return baseItr.hasNext();
        }

        @Override
        public Object next() {
            return baseItr.next().get(index);
        }
    }
}
