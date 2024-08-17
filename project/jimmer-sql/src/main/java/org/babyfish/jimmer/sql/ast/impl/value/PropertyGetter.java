package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface PropertyGetter extends ValueGetter {

    @Nullable
    String alias();

    ImmutableProp prop();

    static List<PropertyGetter> entityGetters(
            JSqlClientImplementor sqlClient,
            ImmutableType type,
            ImmutableSpi entity,
            Predicate<ImmutableProp> propFilter
    ) {
        List<PropertyGetter> propertyGetters = new ArrayList<>();

        ImmutableProp idProp = type.getIdProp();
        if (propFilter == null || propFilter.test(idProp)) {
            PropId idPropId = idProp.getId();
            if (entity == null || entity.__isLoaded(idPropId)) {
                Object value = entity != null ? entity.__get(idPropId) : null;
                propertyGetters.addAll(
                        ScalarPropertyGetter.getters(
                                null,
                                idProp,
                                AbstractValueGetter.createValueGetters(sqlClient, idProp, value)
                        )
                );
            }
        }
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.isId()) {
                continue;
            }
            if (propFilter != null && !propFilter.test(prop)) {
                continue;
            }
            if (prop.isTransient() || prop.isFormula() || prop.isView()) {
                continue;
            }
            if (entity != null && !entity.__isLoaded(prop.getId())) {
                continue;
            }
            Object value = entity != null ? entity.__get(prop.getId()) : null;
            if (prop.isColumnDefinition() && prop.isReference(TargetLevel.ENTITY)) {
                propertyGetters.addAll(
                        ReferencePropertyGetter.getters(
                                null,
                                prop,
                                AbstractValueGetter.createValueGetters(
                                        sqlClient,
                                        prop,
                                        value != null ?
                                                ((ImmutableSpi) value).__get(prop.getTargetType().getIdProp().getId()) :
                                                null
                                )
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
