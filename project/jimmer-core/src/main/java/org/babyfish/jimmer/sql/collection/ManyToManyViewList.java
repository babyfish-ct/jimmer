package org.babyfish.jimmer.sql.collection;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ManyToManyViewList<M, E> implements List<E> {

    private final int deeperPropId;

    private final List<M> middleList;

    public ManyToManyViewList(int deeperPropId, List<M> middleList) {
        this.deeperPropId = deeperPropId;
        this.middleList = middleList;
    }

    @Override
    public boolean isEmpty() {
        return middleList.isEmpty();
    }

    @Override
    public int size() {
        return middleList.size();
    }

    @Override
    public boolean contains(Object o) {
        for (M middle : middleList) {
            if (toElement(middle).equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object target : c) {
            if (!contains(target)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int indexOf(Object o) {
        ListIterator<M> itr = middleList.listIterator(0);
        while (itr.hasNext()) {
            if (itr.next().equals(o)) {
                return itr.nextIndex() - 1;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        ListIterator<M> itr = middleList.listIterator(middleList.size());
        while (itr.hasPrevious()) {
            if (itr.previous().equals(o)) {
                return itr.previousIndex() + 1;
            }
        }
        return -1;
    }

    @NotNull
    @Override
    public Object[] toArray() {
        Object[] arr = new Object[middleList.size()];
        int index = 0;
        for (M middle : middleList) {
            arr[index++] = toElement(middle);
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        int size = middleList.size();
        if (size > a.length) {
            a = (T[])new Object[size];
        }
        int index = 0;
        for (M middle : middleList) {
            a[index++] = (T)toElement(middle);
        }
        return a;
    }

    @Override
    public E get(int index) {
        return toElement(middleList.get(index));
    }

    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new ManyToManyViewList<>(deeperPropId, middleList.subList(fromIndex, toIndex));
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Itr(middleList.listIterator(0));
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return new Itr(middleList.listIterator(0));
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        return new Itr(middleList.listIterator(index));
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (M middle : middleList) {
            hashCode = 31 * hashCode + toElement(middle).hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }
        if (middleList.size() != ((List<?>)o).size()) {
            return false;
        }
        Iterator<M> middleItr = middleList.iterator();
        Iterator<?> otherItr = ((List<?>) o).iterator();
        while (middleItr.hasNext() && otherItr.hasNext()) {
            if (!toElement(middleItr.next()).equals(otherItr.next())) {
                return false;
            }
        }
        return !(middleItr.hasNext() || otherItr.hasNext());
    }

    @Override
    public String toString() {
        Iterator<M> itr = middleList.iterator();
        if (! itr.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        while (true) {
            M middle = itr.next();
            sb.append(toElement(middle));
            if (!itr.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    @SuppressWarnings("unchecked")
    private E toElement(M middle) {
        return (E)((ImmutableSpi)middle).__get(deeperPropId);
    }

    @Deprecated
    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public void clear() {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    private class Itr implements ListIterator<E> {

        private final ListIterator<M> itr;

        private Itr(ListIterator<M> itr) {
            this.itr = itr;
        }

        @Override
        public boolean hasNext() {
            return itr.hasNext();
        }

        @Override
        public E next() {
            return toElement(itr.next());
        }

        @Override
        public boolean hasPrevious() {
            return itr.hasPrevious();
        }

        @Override
        public E previous() {
            return toElement(itr.previous());
        }

        @Override
        public int nextIndex() {
            return itr.nextIndex();
        }

        @Override
        public int previousIndex() {
            return itr.previousIndex();
        }

        @Deprecated
        @Override
        public void remove() {
            throw new UnsupportedOperationException("The current list is immutable");
        }

        @Deprecated
        @Override
        public void set(E e) {
            throw new UnsupportedOperationException("The current list is immutable");
        }

        @Deprecated
        @Override
        public void add(E e) {
            throw new UnsupportedOperationException("The current list is immutable");
        }
    }
}
