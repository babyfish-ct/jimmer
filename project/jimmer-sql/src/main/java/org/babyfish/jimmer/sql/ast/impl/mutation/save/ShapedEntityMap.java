package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class ShapedEntityMap<E> extends SemNode<E> implements Iterable<Batch<E>> {

    private static final ShapedEntityMap<Object> EMPTY =
            new ShapedEntityMap<>(null, null, SaveMode.UPSERT);

    private final JSqlClientImplementor sqlClient;

    private final Set<ImmutableProp> keyProps;

    private static final int CAPACITY = 8;

    private SemNode<E>[] tab;

    private int modCount;

    ShapedEntityMap(JSqlClientImplementor sqlClient, Set<ImmutableProp> keyProps, SaveMode mode) {
        super(0, null, null, mode,null, null, null);
        this.sqlClient = sqlClient;
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
        Shape key = Shape.of(sqlClient, (ImmutableSpi) entity);
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
        SemNode<E> node = new SemNode<>(h, key, entities, mode, startNode, last, this);
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
    Shape shape();
    EntitySet<E> entities();
    default SaveMode mode() {
        return SaveMode.UPSERT;
    }

    static <E> Batch<E> of(Shape shape, EntitySet<E> entities) {
        return new Batch<E>() {
            @Override
            public Shape shape() {
                return shape;
            }
            @Override
            public EntitySet<E> entities() {
                return entities;
            }
        };
    }

    static <E> Batch<E> of(Batch<E> base, SaveMode mode) {
        if (base.mode() == mode) {
            return base;
        }
        return new Batch<E>() {

            @Override
            public Shape shape() {
                return base.shape();
            }

            @Override
            public EntitySet<E> entities() {
                return base.entities();
            }

            @Override
            public SaveMode mode() {
                return mode;
            }
        };
    }
}

class SemNode<E> implements Batch<E> {

    final int hash;
    final Shape key;
    final EntitySet<E> entities;
    final SaveMode mode;
    SemNode<E> next;
    SemNode<E> before;
    SemNode<E> after;

    SemNode(
            int hash,
            Shape key,
            EntitySet<E> entities,
            SaveMode mode,
            SemNode<E> next,
            SemNode<E> before,
            SemNode<E> after
    ) {
        this.hash = hash;
        this.key = key;
        this.entities = entities;
        this.mode = mode;
        this.next = next;
        this.before = before;
        this.after = after;
    }

    @Override
    public Shape shape() {
        return key;
    }

    @Override
    public EntitySet<E> entities() {
        return entities;
    }

    @Override
    public SaveMode mode() {
        return mode;
    }
}