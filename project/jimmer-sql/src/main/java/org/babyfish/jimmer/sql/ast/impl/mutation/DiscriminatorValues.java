package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;

final class DiscriminatorValues {

    private DiscriminatorValues() {}

    static Object of(ImmutableType type) {
        String value = type.getDiscriminatorValue();
        if (value == null) {
            return null;
        }
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        return inheritanceInfo != null ?
                inheritanceInfo.discriminatorValue(value) :
                value;
    }
}
