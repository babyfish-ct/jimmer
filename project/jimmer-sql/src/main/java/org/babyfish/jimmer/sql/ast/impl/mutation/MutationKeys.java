package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class MutationKeys {

    private MutationKeys() {
    }

    @Nullable
    static ImmutableProp activeStateKeyProp(ImmutableType type) {
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        // Unlike boolean flags or generated deletion markers, enum/int flags can have several live values.
        return info != null && info.getAction() instanceof LogicalDeletedInfo.Action.Ne &&
                (info.getType().isEnum() || info.getType() == int.class) ? info.getProp() : null;
    }

    static Collection<ImmutableProp> matchingKeyProps(ImmutableType type, Collection<ImmutableProp> keyProps) {
        ImmutableProp prop = activeStateKeyProp(type);
        if (prop == null || keyProps.contains(prop)) {
            return keyProps;
        }
        List<ImmutableProp> props = new ArrayList<>(keyProps);
        addProp(props, prop);
        return props;
    }

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

    @Nullable
    static LogicalDeletedInfo logicalDeletedConflictPredicate(ImmutableType type) {
        LogicalDeletedInfo logicalDeletedInfo = type.getLogicalDeletedInfo();
        return logicalDeletedInfo != null && logicalDeletedInfo.getType() == boolean.class ?
                logicalDeletedInfo :
                null;
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
