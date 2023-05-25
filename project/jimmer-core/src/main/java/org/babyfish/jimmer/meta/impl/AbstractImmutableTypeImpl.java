package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.sql.meta.impl.MetaCache;
import org.babyfish.jimmer.sql.meta.impl.PropChains;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractImmutableTypeImpl implements ImmutableType {

    private final MetaCache<Map<String, List<ImmutableProp>>> chainMapCache =
            new MetaCache<>(it -> PropChains.of(this, it));

    @Override
    public List<ImmutableProp> getPropChain(String columnName, MetadataStrategy strategy) {
        Map<String, List<ImmutableProp>> chainMap = chainMapCache.get(strategy);
        List<ImmutableProp> chain = chainMap.get(DatabaseIdentifiers.comparableIdentifier(columnName));
        if (chain == null) {
            throw new IllegalArgumentException(
                    "There is no property chain whose column name is \"" +
                            columnName +
                            "\" in type \"" +
                            this +
                            "\""
            );
        }
        return chain;
    }

    public void validateColumns(MetadataStrategy strategy) {
        chainMapCache.get(strategy);
    }
}
