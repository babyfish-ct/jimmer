package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface PropertyGetter extends ValueGetter {

    @Nullable
    String alias();

    ImmutableProp prop();

    static List<PropertyGetter> entityGetters(
            JSqlClientImplementor sqlClient,
            ImmutableType type,
            ImmutableSpi entity,
            boolean includeNonColumnDefinition
    ) {
        List<PropertyGetter> propertyGetters = new ArrayList<>();
        for (ImmutableProp prop : type.getProps().values()) {
            if (!includeNonColumnDefinition && !prop.isColumnDefinition()) {
                continue;
            }
            if (prop.isTransient() || prop.isFormula() || prop.isView()) {
                continue;
            }
            if (entity != null && !entity.__isLoaded(prop.getId())) {
                continue;
            }
            Object value = entity != null ? entity.__get(prop.getId()) : null;
            if (prop.isReference(TargetLevel.ENTITY)) {
                if (value != null) {
                    value = ((ImmutableSpi)value).__get(prop.getTargetType().getIdProp().getId());
                }
                propertyGetters.addAll(
                        ReferencePropertyGetter.getters(
                                null,
                                prop,
                                AbstractValueGetter.createValueGetters(sqlClient, prop, value)
                        )
                );
            } else {
                propertyGetters.addAll(
                        ScalarPropertyGetter.getters(
                                null,
                                prop,
                                AbstractValueGetter.createValueGetters(sqlClient, prop, value)
                        )
                );
            }
        }
        return propertyGetters;
    }

    static List<PropertyGetter> propertyGetters(JSqlClientImplementor sqlClient, ImmutableProp prop) {
        if (prop.isReference(TargetLevel.ENTITY)) {
            return ReferencePropertyGetter.getters(
                    null,
                    prop,
                    AbstractValueGetter.createValueGetters(sqlClient, prop, null)
            );
        }
        return ScalarPropertyGetter.getters(
                null,
                prop,
                AbstractValueGetter.createValueGetters(sqlClient, prop, null)
        );
    }
}
