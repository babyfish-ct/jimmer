package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.jetbrains.annotations.NotNull;

import java.util.*;

class EntityList<E> implements EntityCollection<E> {

    private final List<E> entities;

    public EntityList() {
        entities = new ArrayList<>();
    }

    public EntityList(int size) {
        entities = new ArrayList<>(size);
    }

    @Override
    public boolean isEmpty() {
        return entities.isEmpty();
    }

    @Override
    public int size() {
        return entities.size();
    }

    @Override
    public boolean contains(Object o) {
        return entities.contains(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return entities.contains(c);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return entities.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return entities.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return entities.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return entities.add(e);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        return this.entities.addAll(c);
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

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Item<E>> items() {
        return new Iterable<Item<E>>() {
            @NotNull
            @Override
            public Iterator<Item<E>> iterator() {
                return new ItemItr<>(entities.iterator());
            }

            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                builder.append('[');
                boolean addComma = false;
                for (Item<E> item : this) {
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
        };
    }

    private static class ItemItr<E> implements Iterator<Item<E>> {

        private final Iterator<E> itr;

        private ItemItr(Iterator<E> itr) {
            this.itr = itr;
        }

        @Override
        public boolean hasNext() {
            return itr.hasNext();
        }

        @Override
        public Item<E> next() {
            return new ItemImpl<>(itr.next());
        }
    }

    private static class ItemImpl<E> implements Item<E> {

        private final E data;

        private ItemImpl(E data) {
            this.data = data;
        }

        @Override
        public E getEntity() {
            return data;
        }

        @Override
        public Iterable<E> getOriginalEntities() {
            return new Iterable<E>() {
                @NotNull
                @Override
                public Iterator<E> iterator() {
                    return new SingleItr<>(data);
                }
            };
        }

        @Override
        public String toString() {
            return "{entity:" +
                    data +
                    "}";
        }
    }

    private static class SingleItr<E> implements Iterator<E> {

        private E data;

        private SingleItr(E data) {
            this.data = data;
        }

        @Override
        public boolean hasNext() {
            return data != null;
        }

        @Override
        public E next() {
            E e = this.data;
            if (e == null) {
                throw new NoSuchElementException();
            }
            this.data = null;
            return e;
        }
    }
}
