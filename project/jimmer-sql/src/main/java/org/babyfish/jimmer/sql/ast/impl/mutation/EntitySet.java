package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;

class EntitySet<E> extends EsNode<E> implements EntityCollection<E> {

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
                node.merge(data);
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
        boolean modified = false;
        for (E e : c) {
            modified |= add(e);
        }
        return modified;
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
    public Iterator<E> iterator() {
        if (after == this) {
            return Collections.emptyIterator();
        }
        return new EntityItr<>(this);
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
    public Iterable<Item<E>> items() {
        return new Items();
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

    private static abstract class AbstractItr<E> {

        private final EntitySet<E> owner;

        private int modCount;

        private EsNode<E> current;

        private EsNode<E> ret;

        public AbstractItr(EntitySet<E> owner) {
            this.owner = owner;
            modCount = owner.modCount;
            current = owner.after;
        }

        public final boolean hasNext() {
            if (owner.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            return current != owner;
        }

        protected final EntityCollection.Item<E> nextItem() {
            if (owner.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (current == owner) {
                throw new NoSuchElementException();
            }
            EntityCollection.Item<E> item = current;
            ret = current;
            current = current.after;
            return item;
        }

        public final void remove() {
            if (owner.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (ret == null) {
                throw new IllegalStateException();
            }
            int index = (CAPACITY - 1) & ret.hash;
            EsNode<E> prev = null;
            for (EsNode<E> n = owner.tab[index]; n != null; n = n.next) {
                if (n == ret) {
                    if (prev != null) {
                        prev.next = n.next;
                    } else {
                        owner.tab[index] = n.next;
                    }
                    break;
                }
                prev = n;
            }
            ret.before.after = ret.after;
            ret.after.before = ret.before;
            owner.size--;
            modCount = ++owner.modCount;
        }
    }

    private static class EntityItr<E> extends AbstractItr<E> implements Iterator<E> {

        public EntityItr(EntitySet<E> owner) {
            super(owner);
        }

        @Override
        public E next() {
            return nextItem().getEntity();
        }
    }

    private static class ItemItr<E> extends AbstractItr<E> implements Iterator<EntityCollection.Item<E>> {

        public ItemItr(EntitySet<E> owner) {
            super(owner);
        }

        @Override
        public EntityCollection.Item<E> next() {
            return nextItem();
        }
    }

    private class Items implements Iterable<Item<E>> {

        @NotNull
        @Override
        public Iterator<Item<E>> iterator() {
            return new ItemItr<>(EntitySet.this);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            boolean addComma = false;
            for (Item<E> item : items()) {
                if (addComma) {
                    builder.append(", ");
                } else {
                    addComma = true;
                }
                builder.append(item);
            }
            builder.append(']');
            return builder.toString();
        }
    }
}

class EsNode<E> implements EntityCollection.Item<E> {

    final int hash;
    E data;
    E[] originalArr;
    int originalCount;
    EsNode<E> next;
    EsNode<E> before;
    EsNode<E> after;

    EsNode(int hash, E data, EsNode<E> next, EsNode<E> before, EsNode<E> after) {
        this.hash = hash;
        this.data = data;
        this.originalArr = null;
        this.originalCount = 1;
        this.next = next;
        this.before = before;
        this.after = after;
    }

    @SuppressWarnings("unchecked")
    void merge(E data) {
        if (this.data == data) {
            return;
        }
        int arrLen = originalArr == null ? 1 : originalArr.length;
        if (originalCount >= arrLen) {
            E[] arr = (E[])new Object[arrLen * 2];
            if (arrLen == 1) {
                arr[0] = this.data;
            } else {
                System.arraycopy(originalArr, 0, arr, 0, arrLen);
            }
            this.originalArr = arr;
        }
        assert originalArr != null;
        originalArr[originalCount++] = data;
        this.data = data;
    }

    @Override
    public E getEntity() {
        return data;
    }

    @Override
    public Iterable<E> getOriginalEntities() {
        return new Original();
    }

    @Override
    public String toString() {
        if (originalArr == null) {
            return "{\"entity\":" +
                    data +
                    "}";
        }
        return "{entity:" +
                data +
                ",originalEntities:" +
                getOriginalEntities() +
                "}";
    }

    private class Original implements Iterable<E> {

        @NotNull
        @Override
        public Iterator<E> iterator() {
            if (originalArr == null) {
                return new SingleItr<>(data);
            }
            return new MultipleItr<>(originalArr, originalCount);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            boolean addComma = false;
            for (E e : this) {
                if (addComma) {
                    builder.append(", ");
                } else {
                    addComma = true;
                }
                builder.append(e);
            }
            builder.append(']');
            return builder.toString();
        }
    }

    private static class SingleItr<E> implements Iterator<E> {

        private E data;

        SingleItr(E data) {
            this.data = data;
        }

        @Override
        public boolean hasNext() {
            return data != null;
        }

        @Override
        public E next() {
            E data = this.data;
            if (data == null) {
                throw new NoSuchElementException();
            }
            this.data = null;
            return data;
        }
    }

    private static class MultipleItr<E> implements Iterator<E> {

        private final E[] arr;

        private final int len;

        private int index;

        MultipleItr(E[] arr, int len) {
            this.arr = arr;
            this.len = len;
        }

        @Override
        public boolean hasNext() {
            return index < len;
        }

        @Override
        public E next() {
            if (index >= len) {
                throw new NoSuchElementException();
            }
            return arr[index++];
        }
    }
}