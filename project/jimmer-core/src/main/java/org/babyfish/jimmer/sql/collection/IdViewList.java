package org.babyfish.jimmer.sql.collection;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public final class IdViewList<E, ID> extends AbstractIdViewList<E, ID> {

    public IdViewList(ImmutableType entityType, List<E> entityList) {
        super(entityType, entityList);
    }

    @NotNull
    @Override
    public List<ID> subList(int fromIndex, int toIndex) {
        return new IdViewList<>(entityType, entityList);
    }

    @NotNull
    @Override
    public Iterator<ID> iterator() {
        return new Itr<>(this, entityList.listIterator(0));
    }

    @NotNull
    @Override
    public ListIterator<ID> listIterator() {
        return new Itr<>(this, entityList.listIterator(0));
    }

    @NotNull
    @Override
    public ListIterator<ID> listIterator(int index) {
        return new Itr<>(this, entityList.listIterator(index));
    }

    @Deprecated
    @Override
    public boolean add(ID e) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public void add(int index, ID element) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public boolean addAll(@NotNull Collection<? extends ID> c) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    @Deprecated
    @Override
    public boolean addAll(int index, @NotNull Collection<? extends ID> c) {
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
    public ID remove(int index) {
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

    @Deprecated
    @Override
    public ID set(int index, ID element) {
        throw new UnsupportedOperationException("The current list is immutable");
    }

    static class Itr<E, ID> extends AbstractIdViewList.Itr<E, ID> {

        Itr(AbstractIdViewList<E, ID> owner, ListIterator<E> itr) {
            super(owner, itr);
        }

        @Deprecated
        @Override
        public void remove() {
            throw new UnsupportedOperationException("The current list is immutable");
        }

        @Deprecated
        @Override
        public void set(ID e) {
            throw new UnsupportedOperationException("The current list is immutable");
        }

        @Deprecated
        @Override
        public void add(ID e) {
            throw new UnsupportedOperationException("The current list is immutable");
        }
    }
}
