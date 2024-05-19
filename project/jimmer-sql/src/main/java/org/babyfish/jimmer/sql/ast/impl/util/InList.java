package org.babyfish.jimmer.sql.ast.impl.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class InList<E> implements Iterable<Iterable<E>> {

    private final Collection<E> values;

    private final boolean padding;

    private final int maximum;

    private CommitterImpl<E> committer;

    public InList(Collection<E> values, boolean padding, int maximum) {
        this.values = values;
        this.padding = padding;
        this.maximum = maximum;
    }

    public Committer committer() {
        return this.committer = new CommitterImpl<>();
    }

    @NotNull
    @Override
    public Iterator<Iterable<E>> iterator() {
        CommitterImpl<E> c = committer;
        if (c != null && c.owner != null) {
            throw new IllegalStateException("Iterator requires no committer or new comitter");
        }
        return new Itr(values.iterator(), c);
    }

    public interface Committer {
        void commit();
    }

    private class Itr implements Iterator<Iterable<E>> {

        private final Iterator<E> rawItr;

        private final CommitterImpl<E> committer;

        private Itr(Iterator<E> rawItr, CommitterImpl<E> committer) {
            this.rawItr = rawItr;
            this.committer = committer;
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
                    return new NestedItr(rawItr, committer);
                }
            };
        }
    }

    private class NestedItr implements Iterator<E> {

        private final Iterator<E> rawItr;

        private final CommitterImpl<E> committer;

        private int visited;

        private E value;

        private NestedItr(Iterator<E> rawItr, CommitterImpl<E> committer) {
            this.rawItr = rawItr;
            this.committer = committer;
            if (committer != null) {
                committer.owner = this;
            }
        }

        @Override
        public boolean hasNext() {
            CommitterImpl<E> c = committer;
            int committed = c != null ? c.count : visited;
            if (committed >= maximum) {
                if (c != null) {
                    c.reset();
                }
                return false;
            }
            if (rawItr.hasNext()) {
                return true;
            }
            if (c != null) {
                c.frozen = true;
            }
            boolean has = InList.this.padding && (committed & committed - 1) != 0;
            if (!has && c != null) {
                c.reset();
            }
            return has;
        }

        @Override
        public E next() {
            CommitterImpl<E> c = committer;
            visited++;
            if (c != null && c.frozen) {
                c.count++;
            }
            if (!rawItr.hasNext()) {
                return value;
            }
            if (c != null) {
                return c.value = rawItr.next();
            }
            return value = rawItr.next();
        }
    }

    private static class CommitterImpl<E> implements Committer {

        int count;

        boolean frozen;

        E value;

        InList<E>.NestedItr owner;

        @Override
        public void commit() {
            if (!frozen) {
                count++;
                owner.value = value;
            }
        }

        void reset() {
            count = 0;
            frozen = false;
        }
    }
}
