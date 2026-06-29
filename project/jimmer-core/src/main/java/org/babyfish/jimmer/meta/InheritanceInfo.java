package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JoinedTableDissociateAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class InheritanceInfo {

    private final ImmutableType rootType;

    private final InheritanceType strategy;

    private final JoinedTableDissociateAction joinedTableDissociateAction;

    private final ImmutableProp discriminatorProp;

    public InheritanceInfo(
            ImmutableType rootType,
            InheritanceType strategy,
            JoinedTableDissociateAction joinedTableDissociateAction,
            ImmutableProp discriminatorProp
    ) {
        this.rootType = rootType;
        this.strategy = strategy;
        this.joinedTableDissociateAction = joinedTableDissociateAction;
        this.discriminatorProp = discriminatorProp;
    }

    public ImmutableType getRootType() {
        return rootType;
    }

    public InheritanceType getStrategy() {
        return strategy;
    }

    public JoinedTableDissociateAction getJoinedTableDissociateAction() {
        return joinedTableDissociateAction;
    }

    public ImmutableProp getDiscriminatorProp() {
        return discriminatorProp;
    }

    @NotNull
    public ImmutableProp getDiscriminatorProp(@NotNull ImmutableType type) {
        if (!rootType.isAssignableFrom(type)) {
            throw new IllegalArgumentException(
                    "The type \"" + type + "\" does not belong to the inheritance hierarchy of \"" +
                            rootType +
                            "\""
            );
        }
        ImmutableProp prop = type.getProps().get(discriminatorProp.getName());
        if (prop == null ||
                !prop.isDiscriminator() ||
                prop.toOriginal() != discriminatorProp.toOriginal()) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            type +
                            "\" does not have the discriminator property \"" +
                            discriminatorProp.getName() +
                            "\" of \"" +
                            rootType +
                            "\""
            );
        }
        return prop;
    }

    public Collection<ImmutableType> getConcreteTypes() {
        return getConcreteTypes(rootType);
    }

    public Collection<ImmutableType> getConcreteTypes(@NotNull ImmutableType baseType) {
        if (!rootType.isAssignableFrom(baseType)) {
            throw new IllegalArgumentException(
                    "The type \"" + baseType + "\" does not belong to the inheritance hierarchy of \"" +
                            rootType +
                            "\""
            );
        }
        Set<ImmutableType> types = new LinkedHashSet<>();
        if (baseType.isInstantiable()) {
            types.add(baseType);
        }
        types.addAll(baseType.getAllDerivedTypes());
        types.removeIf(type -> !type.isInstantiable());
        return Collections.unmodifiableSet(types);
    }

    public Map<Object, ImmutableType> getDiscriminatorTypeMap() {
        Map<Object, ImmutableType> map = new LinkedHashMap<>();
        for (ImmutableType type : getConcreteTypes()) {
            String value = type.getDiscriminatorValue();
            if (value != null) {
                map.put(discriminatorValue(value), type);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object discriminatorValue(String value) {
        Class<?> propType = discriminatorProp.getReturnClass();
        if (propType.isEnum()) {
            return Enum.valueOf((Class<Enum>) propType, value);
        }
        return value;
    }

    @Override
    public String toString() {
        return "InheritanceInfo{" +
                "rootType=" + rootType +
                ", strategy=" + strategy +
                ", joinedTableDissociateAction=" + joinedTableDissociateAction +
                ", discriminatorProp=" + discriminatorProp +
                '}';
    }
}
