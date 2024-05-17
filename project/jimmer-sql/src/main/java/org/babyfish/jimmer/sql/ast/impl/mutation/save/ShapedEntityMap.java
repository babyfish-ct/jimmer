package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class ShapedEntityMap<E> extends SemNode<E> implements Iterable<EntitySet<E>> {

    private static final int CAPACITY = 8;

    private SemNode<E>[] tab;

    private int modCount;

    public ShapedEntityMap() {
        super(0, null, null, null, null, null);
        before = this;
        after = this;
    }

    @SuppressWarnings("unchecked")
    public void add(E entity) {
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
        if (((ImmutableSpi) entity).__isLoaded(idPropId)) {
            entities = new EntitySet<>(new PropId[]{ idPropId });
        } else {
            Set<ImmutableProp> keyProps = keyProps(key.getType());
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

    public Batch<E> remove() {
        SemNode<E> node = this.after;
        if (node == this) {
            return null;
        }
        SemNode<E>[] tab = this.tab;
        node.before.after = node.after;
        node.after.before = node.before;
        int index = node.hash & (CAPACITY - 1);
        for (SemNode<E> p = null, n = tab[index]; n != null; p = n, n = n.next) {
            if (node == n) {
                if (p != null) {
                    p.next = n.next;
                } else {
                    tab[index] = n.next;
                }
                break;
            }
        }
        modCount++;
        return node;
    }

    public boolean isEmpty() {
        return after == this;
    }

    @NotNull
    @Override
    public Iterator<EntitySet<E>> iterator() {
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

    protected Set<ImmutableProp> keyProps(ImmutableType type) {
        return type.getKeyProps();
    }

    private class Itr implements Iterator<EntitySet<E>> {

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
        public EntitySet<E> next() {
            if (ShapedEntityMap.this.modCount != modCount) {
                throw new ConcurrentModificationException();
            }
            if (current == ShapedEntityMap.this) {
                throw new NoSuchElementException();
            }
            EntitySet<E> entities = current.entities;
            current = current.after;
            return entities;
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
        return shape();
    }

    @Override
    public EntitySet<E> entities() {
        return entities;
    }
}