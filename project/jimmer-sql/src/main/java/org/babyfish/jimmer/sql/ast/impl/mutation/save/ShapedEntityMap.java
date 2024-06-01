package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class ShapedEntityMap<E> extends SemNode<E> implements Iterable<Batch<E>> {

    private static final ShapedEntityMap<Object> EMPTY = new ShapedEntityMap<>(null);

    private final Set<ImmutableProp> keyProps;

    private static final int CAPACITY = 8;

    private SemNode<E>[] tab;

    private int modCount;

    ShapedEntityMap(Set<ImmutableProp> keyProps) {
        super(0, null, null, null, null, null);
        this.keyProps = keyProps;
        before = this;
        after = this;
    }

    @SuppressWarnings("unchecked")
    void add(E entity) {
        if (this == EMPTY) {
            throw new UnsupportedOperationException("The empty shaped entity map is readonly");
        }
        if (tab == null) {
            tab = new SemNode[CAPACITY];
        }
        SaveShape key = SaveShape.of((ImmutableSpi) entity);
        int h = key.hashCode();
        h = h ^ (h >>> 16);
        int index = (CAPACITY - 1) & h;
        SemNode<E> startNode = tab[index];
        for (SemNode<E> node = startNode; node != null; node = node.next) {
            if (node.hash == h && node.key.equals(key)) {
                node.entities.add(entity);
                modCount++;
                return;
            }
        }
        PropId idPropId = key.getType().getIdProp().getId();
        EntitySet<E> entities;
        if (((ImmutableSpi)entity).__isLoaded(idPropId)) {
            entities = new EntitySet<E>(new PropId[]{ idPropId });
        } else {
            Set<ImmutableProp> keyProps = this.keyProps;
            PropId[] keyPropIds = new PropId[keyProps.size()];
            int i = 0;
            for (ImmutableProp keyProp : keyProps) {
                keyPropIds[i++] = keyProp.getId();
            }
            entities = new EntitySet<>(keyPropIds);
        }
        entities.add(entity);
        SemNode<E> last = before;
        SemNode<E> node = new SemNode<>(h, key, entities, startNode, last, this);
        last.after = node;
        before = node;
        tab[index] = node;
        modCount++;
    }

    boolean isEmpty() {
        return after == this;
    }

    @NotNull
    @Override
    public Iterator<Batch<E>> iterator() {
        if (after == this) {
            return Collections.emptyIterator();
        }
        return new Itr();
    }

    @Override
    public String toString() {
        if (after == this) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        boolean addComma = false;
        for (SemNode<E> n = after; n != this; n = n.after) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
            }
            builder.append(n.key).append(": ").append(n.entities);
        }
        builder.append('}');
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    static <E> ShapedEntityMap<E> empty() {
        return (ShapedEntityMap<E>) EMPTY;
    }

    private class Itr implements Iterator<Batch<E>> {

        private final int modCount;

        private SemNode<E> current;

        public Itr() {
            modCount = ShapedEntityMap.this.modCount;
            current = after;
        }

        @Override
        public boolean hasNext() {
            if (ShapedEntityMap.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            return current != ShapedEntityMap.this;
        }

        @Override
        public Batch<E> next() {
            if (ShapedEntityMap.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (current == ShapedEntityMap.this) {
                throw new NoSuchElementException();
            }
            Batch<E> batch = current;
            current = current.after;
            return batch;
        }
    }
}

interface Batch<E> {
    SaveShape shape();
    EntitySet<E> entities();
    static <E> Batch<E> of(SaveShape shape, EntitySet<E> entities) {
        return new Batch<E>() {
            @Override
            public SaveShape shape() {
                return shape;
            }
            @Override
            public EntitySet<E> entities() {
                return entities;
            }
        };
    }
}

class SemNode<E> implements Batch<E> {

    final int hash;
    final SaveShape key;
    final EntitySet<E> entities;
    SemNode<E> next;
    SemNode<E> before;
    SemNode<E> after;

    SemNode(int hash, SaveShape key, EntitySet<E> entities, SemNode<E> next, SemNode<E> before, SemNode<E> after) {
        this.hash = hash;
        this.key = key;
        this.entities = entities;
        this.next = next;
        this.before = before;
        this.after = after;
    }

    @Override
    public SaveShape shape() {
        return key;
    }

    @Override
    public EntitySet<E> entities() {
        return entities;
    }
}