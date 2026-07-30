package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftSpi;

import java.util.*;

class SaveResultCoverage {

    private final Map<DraftSpi, Set<ImmutableProp>> propMap = new IdentityHashMap<>();

    void add(DraftSpi draft, Collection<ImmutableProp> props) {
        Set<ImmutableProp> coveredProps = propMap.computeIfAbsent(
                draft,
                it -> Collections.newSetFromMap(new IdentityHashMap<>())
        );
        for (ImmutableProp prop : props) {
            coveredProps.add(prop.toOriginal());
        }
    }

    boolean contains(DraftSpi draft, ImmutableProp prop) {
        Set<ImmutableProp> props = propMap.get(draft);
        return props != null && props.contains(prop.toOriginal());
    }

    boolean containsAny(DraftSpi draft) {
        return propMap.containsKey(draft);
    }
}
