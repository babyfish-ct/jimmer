package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.*;

class NoTargetEntityIdPairsImpl implements IdPairs.Retain {

    private List<Tuple2<Object, Collection<Object>>> entries;

    public NoTargetEntityIdPairsImpl(Collection<? extends ImmutableSpi> entities) {
        if (entities.isEmpty()) {
            this.entries = Collections.emptyList();
        } else {
            List<Tuple2<Object, Collection<Object>>> entries = new ArrayList<>(entities.size());
            ImmutableType type = entities.iterator().next().__type();
            PropId idPropId = type.getIdProp().getId();
            for (ImmutableSpi entity : entities) {
                Object sourceId = entity.__get(idPropId);
                entries.add(new Tuple2<>(sourceId, Collections.emptyList()));
            }
            this.entries = Collections.unmodifiableList(entries);
        }
    }

    public NoTargetEntityIdPairsImpl(IdPairs idPairs) {
        List<Tuple2<Object, Collection<Object>>> newEntries = new ArrayList<>(idPairs.entries().size());
        for (Tuple2<Object, Collection<Object>> idTuple : idPairs.entries()) {
            newEntries.add(new Tuple2<>(idTuple.get_1(), Collections.emptyList()));
        }
        this.entries = Collections.unmodifiableList(newEntries);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Tuple2<Object, Object>> tuples() {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Tuple2<Object, Collection<Object>>> entries() {
        return entries;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String toString() {
        return "NoTargetEntityIdPairsImpl" + entries();
    }
}
