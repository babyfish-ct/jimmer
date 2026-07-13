package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class SaveReturningUpdate {

    @Nullable
    final PropertyGetter discriminatorGetter;

    final List<PropertyGetter> nullGetters;

    final List<SaveAssignment> assignments;

    SaveReturningUpdate(
            @Nullable PropertyGetter discriminatorGetter,
            List<PropertyGetter> nullGetters,
            List<SaveAssignment> assignments
    ) {
        this.discriminatorGetter = discriminatorGetter;
        this.nullGetters = nullGetters;
        this.assignments = assignments;
    }
}
