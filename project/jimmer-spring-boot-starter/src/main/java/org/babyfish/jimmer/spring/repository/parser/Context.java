package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Context {

    private static final Comparator<ImmutableProp> NAME_LEN_DESC_COMPARATOR =
            Comparator
                    .comparing((ImmutableProp prop) -> prop.getName().length())
                    .reversed();
    
    private Map<ImmutableType, List<ImmutableProp>> orderedPropMap =
            new HashMap<>();

    public List<ImmutableProp> getOrderedProps(ImmutableType type) {
        return orderedPropMap.computeIfAbsent(type, this::createOrderedProps);
    }
    
    private List<ImmutableProp> createOrderedProps(ImmutableType type) {
        return type
                .getProps()
                .values()
                .stream()
                .filter(it -> !it.isScalarList())
                .sorted(NAME_LEN_DESC_COMPARATOR)
                .collect(Collectors.toList());
    }
}
