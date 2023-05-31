package org.babyfish.jimmer.sql.collection;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.runtime.ListDraft;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class MutableIdViewList<E, ID> extends AbstractIdViewList<E, ID> {

    public MutableIdViewList(ImmutableType entityType, List<E> entityList) {
        super(entityType, entityList);
        if (!(entityList instanceof ListDraft<?>)) {
            throw new IllegalArgumentException("base list of mutable id view list must be list draft");
        }
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
        return entityList.add(toEntity(e));
    }

    @Deprecated
    @Override
    public void add(int index, ID element) {
        entityList.add(index, toEntity(element));
    }

    @Deprecated
    @Override
    public boolean addAll(@NotNull Collection<? extends ID> c) {
        return entityList.addAll(toEntities(c));
    }

    @Deprecated
    @Override
    public boolean addAll(int index, @NotNull Collection<? extends ID> c) {
        return entityList.addAll(index, toEntities(c));
    }

    @Deprecated
    @Override
    public void clear() {
        entityList.clear();
    }

    @Deprecated
    @Override
    public boolean remove(Object o) {
        Iterator<E> itr = entityList.iterator();
        while (itr.hasNext()) {
            E entity = itr.next();
            if (toId(entity).equals(o)) {
                itr.remove();
                return true;
            }
        }
        return false;
    }

    @Deprecated
    @Override
    public ID remove(int index) {
        return toId(entityList.remove(index));
    }

    @Deprecated
    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean removed = false;
        Iterator<E> itr = entityList.iterator();
        while (itr.hasNext()) {
            E entity = itr.next();
            if (c.contains(toId(entity))) {
                itr.remove();
                removed = true;
            }
        }
        return removed;
    }

    @Deprecated
    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        boolean removed = false;
        Iterator<E> itr = entityList.iterator();
        while (itr.hasNext()) {
            E entity = itr.next();
            if (!c.contains(toId(entity))) {
                itr.remove();
                removed = true;
            }
        }
        return removed;
    }

    @Deprecated
    @Override
    public ID set(int index, ID element) {
        return toId(entityList.set(index, toEntity(element)));
    }

    @SuppressWarnings("unchecked")
    private E toEntity(ID id) {
        return (E)Internal.produce(entityType, null, draft -> {
            ((DraftSpi)draft).__set(idPropId, id);
        });
    }

    private Collection<E> toEntities(Collection<? extends ID> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<E> entities = new ArrayList<>(ids.size());
        for (ID id : ids) {
            entities.add(toEntity(id));
        }
        return entities;
    }

    static class Itr<E, ID> extends AbstractIdViewList.Itr<E, ID> {

        Itr(AbstractIdViewList<E, ID> owner, ListIterator<E> itr) {
            super(owner, itr);
        }

        @Deprecated
        @Override
        public void remove() {
            itr.remove();
        }

        @Deprecated
        @Override
        public void set(ID e) {
            itr.set(((MutableIdViewList<E, ID>)owner).toEntity(e));
        }

        @Deprecated
        @Override
        public void add(ID e) {
            itr.add(((MutableIdViewList<E, ID>)owner).toEntity(e));
        }
    }
}
