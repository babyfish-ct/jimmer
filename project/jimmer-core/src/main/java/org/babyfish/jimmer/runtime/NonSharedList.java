package org.babyfish.jimmer.runtime;

import java.util.*;

public class NonSharedList<E> implements List<E> {

    private final List<E> raw;

    NonSharedList(List<E> raw) {
        this.raw = raw;
    }

    public static <E> NonSharedList<E> of(List<E> oldList, List<E> newList) {
        if (newList instanceof NonSharedList<?>) {
            if (oldList == newList) {
                return (NonSharedList<E>) newList;
            }
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
    public int size() {
        return raw.size();
    }

    @Override
    public boolean isEmpty() {
        return raw.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return raw.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return raw.iterator();
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
    public boolean add(E e) {
        return raw.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return raw.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return raw.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return raw.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return raw.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return raw.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return raw.retainAll(c);
    }

    @Override
    public void clear() {
        raw.clear();
    }

    @Override
    public E get(int index) {
        return raw.get(index);
    }

    @Override
    public E set(int index, E element) {
        return raw.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        raw.add(index, element);
    }

    @Override
    public E remove(int index) {
        return raw.remove(index);
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
    public ListIterator<E> listIterator() {
        return raw.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return raw.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return raw.subList(fromIndex, toIndex);
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

    private static class RA<E> extends NonSharedList<E> implements RandomAccess {

        RA(List<E> raw) {
            super(raw);
        }
    }
}
