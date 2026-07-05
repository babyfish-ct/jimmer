package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class MutationKeys {

    private MutationKeys() {}

    static List<ImmutableProp> keyAndLogicalDeletedProps(
            ImmutableType type,
            Collection<ImmutableProp> keyProps
    ) {
        List<ImmutableProp> props = new ArrayList<>(keyProps);
        LogicalDeletedInfo logicalDeletedInfo = type.getLogicalDeletedInfo();
        if (logicalDeletedInfo != null) {
            addProp(props, logicalDeletedInfo.getProp());
        }
        return props;
    }

    private static void addProp(List<ImmutableProp> props, ImmutableProp prop) {
        ImmutableProp originalProp = prop.toOriginal();
        for (ImmutableProp existingProp : props) {
            if (existingProp.toOriginal() == originalProp) {
                return;
            }
        }
        props.add(prop);
    }
}
