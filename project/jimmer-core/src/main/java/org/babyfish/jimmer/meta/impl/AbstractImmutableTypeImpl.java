package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.sql.meta.impl.MetaCache;
import org.babyfish.jimmer.sql.meta.impl.PropChains;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractImmutableTypeImpl implements ImmutableType {

    private final MetaCache<Map<String, List<ImmutableProp>>> chainMapCache =
            new MetaCache<>(it -> PropChains.of(this, it));

    @Override
    public List<ImmutableProp> getPropChain(String columnName, MetadataStrategy strategy, boolean nullable) {
        Map<String, List<ImmutableProp>> chainMap = chainMapCache.get(strategy);
        List<ImmutableProp> chain = chainMap.get(DatabaseIdentifiers.comparableIdentifier(columnName));
        if (chain == null && !nullable) {
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

    public void validateColumnUniqueness(MetadataStrategy strategy) {
        chainMapCache.get(strategy);
    }

    @Nullable
    @Override
    public ImmutableType getInheritanceRoot() {
        return null;
    }

    @Nullable
    @Override
    public InheritanceInfo getInheritanceInfo() {
        return null;
    }

    @Override
    public boolean isInstantiable() {
        return false;
    }

    @Override
    public Set<ImmutableType> getDirectDerivedTypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<ImmutableType> getAllDerivedTypes() {
        return Collections.emptySet();
    }

    @Nullable
    @Override
    public String getDiscriminatorValue() {
        return null;
    }
}
