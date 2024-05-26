package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;

public class EntitySet<E> extends EsNode<E> implements Collection<E> {

    private static final int CAPACITY = 8;

    private final PropId[] propIds;

    private EsNode<E>[] tab;

    private int size;

    private int modCount;

    EntitySet(PropId[] propIds) {
        super(0, null, null, null, null);
        this.propIds = propIds;
        before = this;
        after = this;
    }

    @Override
    public boolean isEmpty() {
        return after == this;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean contains(Object o) {
        if (tab == null) {
            return false;
        }
        int h = h((ImmutableSpi) data);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        EsNode<E> startNode = tab[index];
        for (EsNode<E> node = startNode; node != null; node = node.next) {
            if (node.hash == h && eq((ImmutableSpi) node.data, (ImmutableSpi) data)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean add(E data) {
        if (tab == null) {
            tab = new EsNode[CAPACITY];
        }
        int h = h((ImmutableSpi) data);
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        EsNode<E> startNode = tab[index];
        for (EsNode<E> node = startNode; node != null; node = node.next) {
            if (node.hash == h && eq((ImmutableSpi) node.data, (ImmutableSpi) data)) {
                node.data = data;
                modCount++;
                return false;
            }
        }
        EsNode<E> last = before;
        EsNode<E> node = new EsNode<>(h, data, startNode, last, this);
        last.after = node;
        before = node;
        tab[index] = node;
        modCount++;
        size++;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
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

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator iterator() {
        if (after == this) {
            return Collections.emptyIterator();
        }
        return new Itr();
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        Object[] arr = new Object[size];
        int index = 0;
        for (E e : this) {
            arr[index++] = e;
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T @NotNull [] toArray(@NotNull T[] a) {
        T[] arr = a.length < size ? (T[])Array.newInstance(a.getClass().getComponentType(), size) : a;
        int index = 0;
        for (E e : this) {
            arr[index++] = (T)e;
        }
        return arr;
    }

    public E first() {
        EsNode<E> after = this.after;
        if (after == this) {
            throw new NoSuchElementException();
        }
        return after.data;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        boolean addComma = false;
        for (E e : this) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(e);
        }
        builder.append("]");
        return builder.toString();
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
            Object v2 = b.__get(propIds[i]);
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