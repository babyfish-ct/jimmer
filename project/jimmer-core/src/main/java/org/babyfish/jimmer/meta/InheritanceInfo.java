package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.sql.DiscriminatorColumn;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JoinedTableDeleteMode;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class InheritanceInfo {

    private final ImmutableType rootType;

    private final InheritanceType strategy;

    private final JoinedTableDeleteMode joinedTableDeleteMode;

    @Nullable
    private final DiscriminatorColumn discriminatorColumn;

    public InheritanceInfo(
            ImmutableType rootType,
            InheritanceType strategy,
            JoinedTableDeleteMode joinedTableDeleteMode,
            @Nullable DiscriminatorColumn discriminatorColumn
    ) {
        this.rootType = rootType;
        this.strategy = strategy;
        this.joinedTableDeleteMode = joinedTableDeleteMode;
        this.discriminatorColumn = discriminatorColumn;
    }

    public ImmutableType getRootType() {
        return rootType;
    }

    public InheritanceType getStrategy() {
        return strategy;
    }

    public JoinedTableDeleteMode getJoinedTableDeleteMode() {
        return joinedTableDeleteMode;
    }

    @Nullable
    public DiscriminatorColumn getDiscriminatorColumn() {
        return discriminatorColumn;
    }

    public Collection<ImmutableType> getConcreteTypes() {
        Set<ImmutableType> types = new LinkedHashSet<>();
        types.add(rootType);
        types.addAll(rootType.getAllDerivedTypes());
        return Collections.unmodifiableSet(types);
    }

    public Map<String, ImmutableType> getDiscriminatorTypeMap() {
        Map<String, ImmutableType> map = new LinkedHashMap<>();
        for (ImmutableType type : getConcreteTypes()) {
            String value = type.getDiscriminatorValue();
            if (value != null) {
                map.put(value, type);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        return "InheritanceInfo{" +
                "rootType=" + rootType +
                ", strategy=" + strategy +
                ", joinedTableDeleteMode=" + joinedTableDeleteMode +
                ", discriminatorColumn=" + (discriminatorColumn != null ? discriminatorColumn.name() : null) +
                '}';
    }
}
