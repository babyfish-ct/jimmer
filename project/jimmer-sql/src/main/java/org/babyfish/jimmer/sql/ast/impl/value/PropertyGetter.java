package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.ArrayList;
import java.util.List;

public interface PropertyGetter extends ValueGetter {

    ImmutableProp prop();

    static List<PropertyGetter> entityGetters(
            JSqlClientImplementor sqlClient,
            ImmutableType type,
            ImmutableSpi entity
    ) {
        List<PropertyGetter> propertyGetters = new ArrayList<>();
        for (ImmutableProp prop : type.getProps().values()) {
            if (!prop.isColumnDefinition()) {
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
                                prop,
                                AbstractValueGetter.createValueGetters(sqlClient, prop, value)
                        )
                );
            } else {
                propertyGetters.addAll(
                        ScalarPropertyGetter.getters(
                                prop,
                                AbstractValueGetter.createValueGetters(sqlClient, prop, value)
                        )
                );
            }
        }
        return propertyGetters;
    }
}
