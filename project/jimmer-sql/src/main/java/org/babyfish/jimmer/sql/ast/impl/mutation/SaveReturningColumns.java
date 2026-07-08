package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;

import java.util.List;

class SaveReturningColumns {

    final List<PropertyGetter> getters;

    final List<ImmutableProp> props;

    final List<Integer> matchIndexes;

    final int logicalDeletedIndex;

    SaveReturningColumns(
            List<PropertyGetter> getters,
            List<ImmutableProp> props,
            List<Integer> matchIndexes,
            int logicalDeletedIndex
    ) {
        this.getters = getters;
        this.props = props;
        this.matchIndexes = matchIndexes;
        this.logicalDeletedIndex = logicalDeletedIndex;
    }
}
