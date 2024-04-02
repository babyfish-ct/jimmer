package org.babyfish.jimmer.sql.ast.impl.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class InList<E> implements Iterable<Iterable<E>> {

    private final Collection<E> values;

    private final boolean padding;

    private final int maximum;

    public InList(Collection<E> values, boolean padding, int maximum) {
        this.values = values;
        this.padding = padding;
        this.maximum = maximum;
    }

    @NotNull
    @Override
    public Iterator<Iterable<E>> iterator() {
        return new Itr(values.iterator());
    }

    private class Itr implements Iterator<Iterable<E>> {

        private final Iterator<E> rawItr;

        private Itr(Iterator<E> rawItr) {
            this.rawItr = rawItr;
        }

        @Override
        public boolean hasNext() {
            return rawItr.hasNext();
        }

        @Override
        public Iterable<E> next() {
            return new Iterable<E>() {
                @NotNull
                @Override
                public Iterator<E> iterator() {
                    return new NestedItr(rawItr);
                }
            };
        }
    }

    private class NestedItr implements Iterator<E> {

        private final Iterator<E> rawItr;

        private int visited;

        private E value;

        private NestedItr(Iterator<E> rawItr) {
            this.rawItr = rawItr;
        }

        @Override
        public boolean hasNext() {
            int v = visited;
            if (v >= maximum) {
                return false;
            }
            if (rawItr.hasNext()) {
                return true;
            }
            return padding && (v & v - 1) != 0;
        }

        @Override
        public E next() {
            visited++;
            if (!rawItr.hasNext()) {
                return value;
            }
            return value = rawItr.next();
        }
    }
}
