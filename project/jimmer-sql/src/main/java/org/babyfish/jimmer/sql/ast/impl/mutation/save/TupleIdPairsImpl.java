package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.*;

class TupleIdPairsImpl implements IdPairs {

    private final Collection<Tuple2<Object, Object>> tuples;

    private Collection<Tuple2<Object, Collection<Object>>> entries;

    TupleIdPairsImpl(Collection<Tuple2<Object, Object>> tuples) {
        this.tuples = Collections.unmodifiableCollection(tuples);
    }

    @Override
    public Collection<Tuple2<Object, Object>> tuples() {
        return tuples;
    }

    @Override
    public Collection<Tuple2<Object, Collection<Object>>> entries() {
        Collection<Tuple2<Object, Collection<Object>>> entries = this.entries;
        if (entries == null) {
            Map<Object, List<Object>> multiMap = new LinkedHashMap<>();
            for (Tuple2<Object, Object> tuple : tuples) {
                multiMap.computeIfAbsent(tuple.get_1(), it -> new ArrayList<>())
                        .add(tuple.get_2());
            }
            entries = new ArrayList<>(multiMap.size());
            for (Map.Entry<Object, List<Object>> e : multiMap.entrySet()) {
                entries.add(new Tuple2<>(e.getKey(), Collections.unmodifiableList(e.getValue())));
            }
            this.entries = entries = Collections.unmodifiableCollection(entries);
        }
        return entries;
    }

    @Override
    public String toString() {
        return "TupleIdPairs" + tuples();
    }
}
