package org.babyfish.jimmer.runtime;

import java.io.Serializable;
import java.util.*;

public class NonSharedList<E> implements List<E>, Serializable {

    private static final String MUTATION_ERROR_MESSAGE =
            "The list used by immutable object cannot be mutated";

    private final List<E> raw;

    NonSharedList(List<E> raw) {
        this.raw = raw;
    }

    public static <E> NonSharedList<E> of(NonSharedList<E> oldList, List<E> newList) {
        if (oldList == newList) {
            return oldList;
        }
        if (newList instanceof NonSharedList<?>) {
            return of(oldList, ((NonSharedList<E>) newList).raw);
        }
        if (newList == null || newList.isEmpty()) {
            return newList instanceof RandomAccess ?
                    new RA<>(Collections.emptyList()) :
                    new NonSharedList<>(Collections.emptyList());
        }
        return newList instanceof RandomAccess ?
                new RA<>(newList) :
                new NonSharedList<>(newList);
    }

    @Override
    public boolean isEmpty() {
        return raw.isEmpty();
    }

    @Override
    public int size() {
        return raw.size();
    }

    @Override
    public boolean contains(Object o) {
        return raw.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return raw.containsAll(c);
    }

    @Override
    public int indexOf(Object o) {
        return raw.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return raw.lastIndexOf(o);
    }

    @Override
    public E get(int index) {
        return raw.get(index);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
    }

    @Override
    public Object[] toArray() {
        return raw.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return raw.toArray(a);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return raw.equals(o);
    }

    @Override
    public String toString() {
        return raw.toString();
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr<>(raw.iterator());
    }

    @Override
    public ListIterator<E> listIterator() {
        return new ListItr<>(raw.listIterator());
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new ListItr<>(raw.listIterator(index));
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        List<E> subList = raw.subList(fromIndex, toIndex);
        if (subList instanceof RandomAccess) {
            return new RA<>(subList);
        }
        return new NonSharedList<>(subList);
    }

    private static class RA<E> extends NonSharedList<E> implements RandomAccess {

        RA(List<E> raw) {
            super(raw);
        }
    }

    private static class Itr<E> implements Iterator<E> {

        private final Iterator<E> raw;

        private Itr(Iterator<E> raw) {
            this.raw = raw;
        }

        @Override
        public boolean hasNext() {
            return raw.hasNext();
        }

        @Override
        public E next() {
            return raw.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
        }
    }

    private static class ListItr<E> implements ListIterator<E> {

        private final ListIterator<E> raw;

        private ListItr(ListIterator<E> raw) {
            this.raw = raw;
        }

        @Override
        public boolean hasNext() {
            return raw.hasNext();
        }

        @Override
        public E next() {
            return raw.next();
        }

        @Override
        public int nextIndex() {
            return raw.nextIndex();
        }

        @Override
        public boolean hasPrevious() {
            return raw.hasPrevious();
        }

        @Override
        public E previous() {
            return raw.previous();
        }

        @Override
        public int previousIndex() {
            return raw.previousIndex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException(MUTATION_ERROR_MESSAGE);
        }
    }
}
