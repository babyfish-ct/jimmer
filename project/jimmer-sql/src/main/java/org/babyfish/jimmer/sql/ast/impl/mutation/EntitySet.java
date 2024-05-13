package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class EntitySet<E> extends EsNode<E> implements Iterable<E> {

    private static final int CAPACITY = 8;

    private PropId[] propIds;

    private EsNode<E>[] tab;

    private int modCount;

    EntitySet() {
        super(0, null, null, null, null);
        before = this;
        after = this;
    }

    @SuppressWarnings("unchecked")
    public void add(E data) {
        if (tab == null) {
            tab = new EsNode[CAPACITY];
        }
        int h = h((ImmutableSpi) data);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        EsNode<E> startNode = tab[index];
        for (EsNode<E> node = startNode; node != null; node = node.next) {
            if (eq((ImmutableSpi) node.data, (ImmutableSpi) data)) {
                node.data = data;
                modCount++;
                return;
            }
        }
        EsNode<E> last = before;
        EsNode<E> node = new EsNode<>(h, data, startNode, last, this);
        last.after = node;
        before = node;
        tab[index] = node;
        modCount++;
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        if (after == this) {
            return Collections.emptyIterator();
        }
        return new Itr();
    }

    private int h(ImmutableSpi spi) {
        int hash = 1;
        for (int i = propIds.length - 1; i >= 0; --i) {
            Object v = spi.__get(propIds[i]);
            hash = hash * 31 + (v != null ? v.hashCode() : 0);
        }
        return hash;
    }

    private boolean eq(ImmutableSpi a, ImmutableSpi b) {
        for (int i = propIds.length - 1; i >= 0; --i) {
            Object v1 = a.__get(propIds[i]);
            Object v2 = a.__get(propIds[i]);
            if (!Objects.equals(v1, v2)) {
                return false;
            }
        }
        return true;
    }

    private class Itr implements Iterator<E> {

        private final int modCount;

        private EsNode<E> current;

        public Itr() {
            modCount = EntitySet.this.modCount;
            current = after;
        }

        @Override
        public boolean hasNext() {
            if (EntitySet.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            return current != EntitySet.this;
        }

        @Override
        public E next() {
            if (EntitySet.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (current == EntitySet.this) {
                throw new NoSuchElementException();
            }
            E data = current.data;
            current = current.after;
            return data;
        }
    }
}

class EsNode<E> {

    final int hash;
    E data;
    EsNode<E> next;
    EsNode<E> before;
    EsNode<E> after;

    EsNode(int hash, E data, EsNode<E> next, EsNode<E> before, EsNode<E> after) {
        this.hash = hash;
        this.data = data;
        this.next = next;
        this.before = before;
        this.after = after;
    }
}