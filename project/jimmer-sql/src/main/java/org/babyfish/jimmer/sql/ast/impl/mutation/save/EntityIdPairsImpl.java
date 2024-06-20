package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;

public class EntityIdPairsImpl implements IdPairs {

    private final Collection<ImmutableSpi> rows;

    private final PropId propId;

    private final PropId sourceIdPropId;

    private final PropId targetIdProId;

    private final boolean isList;

    private List<Tuple2<Object, Object>> tuples;

    private List<Tuple2<Object, Collection<Object>>> entries;

    public EntityIdPairsImpl(Collection<ImmutableSpi> rows, ImmutableProp prop) {
        if (!prop.isReference(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException(
                    "The property \"" +
                            prop +
                            "\" is not entity association property"
            );
        }
        this.rows = rows;
        this.propId = prop.getId();
        this.sourceIdPropId = prop.getDeclaringType().getIdProp().getId();
        this.targetIdProId = prop.getTargetType().getIdProp().getId();
        this.isList = prop.isReferenceList(TargetLevel.ENTITY);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Tuple2<Object, Object>> tuples() {
        List<Tuple2<Object, Object>> tuples = this.tuples;
        if (tuples == null) {
            tuples = new ArrayList<>();
            for (ImmutableSpi row : rows) {
                Object sourceId = row.__get(sourceIdPropId);
                Object associatedValue = row.__get(propId);
                if (isList) {
                    for (ImmutableSpi e : ((Collection<ImmutableSpi>)associatedValue)) {
                        Object targetId = e.__get(targetIdProId);
                        tuples.add(new Tuple2<>(sourceId, targetId));
                    }
                } else if (associatedValue != null) {
                    Object targetId = ((ImmutableSpi)associatedValue).__get(targetIdProId);
                    tuples.add(new Tuple2<>(sourceId, targetId));
                }
            }
            this.tuples = tuples = Collections.unmodifiableList(tuples);
        }
        return tuples;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Tuple2<Object, Collection<Object>>> entries() {
        List<Tuple2<Object, Collection<Object>>> entries = this.entries;
        if (entries == null) {
            entries = new ArrayList<>(rows.size());
            for (ImmutableSpi row : rows) {
                Object sourceId = row.__get(sourceIdPropId);
                Collection<Object> targetIds;
                Object value = row.__get(propId);
                if (value == null) {
                    targetIds = Collections.emptyList();
                } else if (isList) {
                    List<ImmutableSpi> list = (List<ImmutableSpi>) value;
                    if (list.isEmpty()) {
                        targetIds = Collections.emptyList();
                    } else if (list.size() == 1) {
                        targetIds = Collections.singletonList(list.get(0).__get(targetIdProId));
                    } else {
                        targetIds = new MultipleTargetIdCollection(list, targetIdProId);
                    }
                } else {
                    targetIds = Collections.singletonList(((ImmutableSpi)value).__get(targetIdProId));
                }
                entries.add(new Tuple2<>(sourceId, targetIds));
            }
            this.entries = entries = Collections.unmodifiableList(entries);
        }
        return entries;
    }

    private static class MultipleTargetIdCollection extends AbstractCollection<Object> {

        private final List<ImmutableSpi> associatedRows;

        private final PropId targetIdPropId;

        private MultipleTargetIdCollection(List<ImmutableSpi> associatedRows, PropId targetIdPropId) {
            this.associatedRows = associatedRows;
            this.targetIdPropId = targetIdPropId;
        }

        @Override
        public int size() {
            return associatedRows.size();
        }

        @Override
        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public Iterator<Object> iterator() {
            return new Itr();
        }

        private class Itr implements Iterator<Object> {

            private final Iterator<ImmutableSpi> baseItr;

            Itr() {
                this.baseItr = associatedRows.iterator();
            }

            @Override
            public boolean hasNext() {
                return baseItr.hasNext();
            }

            @Override
            public Object next() {
                return baseItr.next().__get(targetIdPropId);
            }
        }
    }
}
