package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class EntityIdPairsImpl implements IdPairs.Retain {

    private final Collection<? extends ImmutableSpi> rows;

    private final PropId propId;

    private final PropId sourceIdPropId;

    private final PropId targetIdProId;

    private final boolean isList;

    private final Predicate<ImmutableSpi> targetFilter;

    private List<Tuple2<Object, Object>> tuples;

    private List<Tuple2<Object, Collection<Object>>> entries;

    public EntityIdPairsImpl(Collection<? extends ImmutableSpi> rows, ImmutableProp prop) {
        this(rows, prop, it -> true);
    }

    public EntityIdPairsImpl(
            Collection<? extends ImmutableSpi> rows,
            ImmutableProp prop,
            Predicate<ImmutableSpi> targetFilter
    ) {
        if (!prop.isAssociation(TargetLevel.ENTITY)) {
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
        this.targetFilter = targetFilter;
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
                    for (ImmutableSpi e : ((Collection<ImmutableSpi>) associatedValue)) {
                        if (targetFilter.test(e)) {
                            Object targetId = e.__get(targetIdProId);
                            tuples.add(new Tuple2<>(sourceId, targetId));
                        }
                    }
                } else if (associatedValue != null && targetFilter.test((ImmutableSpi) associatedValue)) {
                    Object targetId = ((ImmutableSpi) associatedValue).__get(targetIdProId);
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
                Object value = row.__get(propId);
                Collection<Object> targetIds;
                boolean includeEntry = true;
                if (value == null) {
                    targetIds = Collections.emptyList();
                } else if (isList) {
                    List<ImmutableSpi> list = (List<ImmutableSpi>) value;
                    List<Object> acceptedTargetIds = new ArrayList<>(list.size());
                    for (ImmutableSpi target : list) {
                        if (targetFilter.test(target)) {
                            acceptedTargetIds.add(target.__get(targetIdProId));
                        }
                    }
                    if (acceptedTargetIds.isEmpty()) {
                        targetIds = Collections.emptyList();
                        includeEntry = list.isEmpty();
                    } else if (acceptedTargetIds.size() == 1) {
                        targetIds = Collections.singletonList(acceptedTargetIds.get(0));
                    } else {
                        targetIds = Collections.unmodifiableList(acceptedTargetIds);
                    }
                } else {
                    ImmutableSpi target = (ImmutableSpi) value;
                    if (targetFilter.test(target)) {
                        targetIds = Collections.singletonList(target.__get(targetIdProId));
                    } else {
                        targetIds = Collections.emptyList();
                        includeEntry = false;
                    }
                }
                if (includeEntry) {
                    entries.add(new Tuple2<>(sourceId, targetIds));
                }
            }
            this.entries = entries = Collections.unmodifiableList(entries);
        }
        return entries;
    }

    @Override
    public boolean isEmpty() {
        return entries().isEmpty();
    }

    @Override
    public String toString() {
        return "EntityIdPairs" + entries();
    }

}
