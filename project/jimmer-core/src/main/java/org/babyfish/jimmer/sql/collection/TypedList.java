package org.babyfish.jimmer.sql.collection;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class TypedList<E> implements List<E>, RandomAccess {

    private final String sqlElementType;

    private final E[] arr;

    private final int from;

    private final int to;

    public TypedList(String sqlElementType, E[] arr) {
        this.sqlElementType = sqlElementType;
        this.arr = arr;
        this.from = 0;
        this.to = arr != null ? arr.length : 0;
    }

    private TypedList(String sqlElementType, E[] arr, int from, int to) {
        this.sqlElementType = sqlElementType;
        this.arr = arr;
        this.from = from;
        this.to = to;
    }

    public String getSqlElementType() {
        return sqlElementType;
    }

    @Override
    public int size() {
        return to - from;
    }

    @Override
    public boolean isEmpty() {
        return to == from;
    }

    @Override
    public E get(int index) {
        try {
            return arr[from + index];
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IndexOutOfBoundsException(ex.getMessage());
        }
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
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

    @Override
    public int indexOf(Object o) {
        for (int i = from; i < to; i++) {
            if (Objects.equals(arr[i], o)) {
                return from + i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = to - 1; i >= from; --i) {
            if (Objects.equals(arr[i], o)) {
                return from + i;
            }
        }
        return -1;
    }

    @NotNull
    @Override
    public Object[] toArray() {
        if (from == 0 && to == arr.length) {
            return arr;
        }
        return Arrays.copyOfRange(arr, from, to);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        System.arraycopy((T[])arr, from, a, 0, Math.min(a.length, to - from));
        return a;
    }

    @Deprecated
    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Itr(from);
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return new Itr(from);
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > to - from) {
            throw new IndexOutOfBoundsException();
        }
        return new Itr(from + index);
    }

    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > to - from) {
            throw new IndexOutOfBoundsException();
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("Illegal index range, \"fromIndex\" cannot be greater than \"toIndex\"");
        }
        return new TypedList<>(sqlElementType, arr, from + fromIndex, from + toIndex);
    }

    public class Itr implements ListIterator<E> {

        private int index;

        Itr(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return index < to;
        }

        @Override
        public E next() {
            if (index >= to) {
                throw new NoSuchElementException();
            }
            return arr[index++];
        }

        @Override
        public boolean hasPrevious() {
            return index > from;
        }

        @Override
        public E previous() {
            if (index <= from) {
                throw new NoSuchElementException();
            }
            return arr[--index];
        }

        @Override
        public int nextIndex() {
            return index;
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (int i = from; i < to; i++) {
            E e = arr[i];
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof List<?>)) {
            return false;
        }
        List<?> other = (List<?>) obj;
        if (to - from != other.size()) {
            return false;
        }
        Iterator<?> itr = other.iterator();
        for (int i = from; i < to; i++) {
            if (!Objects.equals(arr[i], itr.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        if (from == to) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = from; i < to; i++) {
            E e = arr[i];
            builder.append(e == this ? "(this Collection)" : e);
            if (i + 1 < to) {
                builder.append(',').append(' ');
            }
        }
        builder.append(']');
        return builder.toString();
    }
}
