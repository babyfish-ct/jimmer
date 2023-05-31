package org.babyfish.jimmer.sql.collection;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class AbstractIdViewList<E, ID> implements List<ID> {

    final ImmutableType entityType;

    final PropId idPropId;

    final List<E> entityList;

    public AbstractIdViewList(ImmutableType entityType, List<E> entityList) {
        this.entityType = entityType;
        this.idPropId = entityType.getIdProp().getId();
        this.entityList = entityList;
    }

    @Override
    public boolean isEmpty() {
        return entityList.isEmpty();
    }

    @Override
    public int size() {
        return entityList.size();
    }

    @Override
    public boolean contains(Object o) {
        for (E entity : entityList) {
            if (toId(entity).equals(o)) {
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
        ListIterator<E> itr = entityList.listIterator(0);
        while (itr.hasNext()) {
            if (itr.next().equals(o)) {
                return itr.nextIndex() - 1;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        ListIterator<E> itr = entityList.listIterator(entityList.size());
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
        Object[] arr = new Object[entityList.size()];
        int index = 0;
        for (E entity : entityList) {
            arr[index++] = toId(entity);
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        int size = entityList.size();
        if (size > a.length) {
            a = (T[])new Object[size];
        }
        int index = 0;
        for (E entity : entityList) {
            a[index++] = (T) toId(entity);
        }
        return a;
    }

    @Override
    public ID get(int index) {
        return toId(entityList.get(index));
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (E entity : entityList) {
            hashCode = 31 * hashCode + toId(entity).hashCode();
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
        if (entityList.size() != ((List<?>)o).size()) {
            return false;
        }
        Iterator<E> entityItr = entityList.iterator();
        Iterator<?> otherItr = ((List<?>) o).iterator();
        while (entityItr.hasNext() && otherItr.hasNext()) {
            if (!toId(entityItr.next()).equals(otherItr.next())) {
                return false;
            }
        }
        return !(entityItr.hasNext() || otherItr.hasNext());
    }

    @Override
    public String toString() {
        Iterator<E> itr = entityList.iterator();
        if (! itr.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        while (true) {
            E entity = itr.next();
            sb.append(toId(entity));
            if (!itr.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    @SuppressWarnings("unchecked")
    ID toId(E entity) {
        return (ID)((ImmutableSpi)entity).__get(idPropId);
    }

    static abstract class Itr<E, ID> implements ListIterator<ID> {

        final AbstractIdViewList<E, ID> owner;

        final ListIterator<E> itr;

        Itr(AbstractIdViewList<E, ID> owner, ListIterator<E> itr) {
            this.owner = owner;
            this.itr = itr;
        }

        @Override
        public boolean hasNext() {
            return itr.hasNext();
        }

        @Override
        public ID next() {
            return owner.toId(itr.next());
        }

        @Override
        public boolean hasPrevious() {
            return itr.hasPrevious();
        }

        @Override
        public ID previous() {
            return owner.toId(itr.previous());
        }

        @Override
        public int nextIndex() {
            return itr.nextIndex();
        }

        @Override
        public int previousIndex() {
            return itr.previousIndex();
        }
    }
}
