package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class SaveReturningUpdate {

    @Nullable
    final PropertyGetter discriminatorGetter;

    final List<PropertyGetter> nullGetters;

    SaveReturningUpdate(
            @Nullable PropertyGetter discriminatorGetter,
            List<PropertyGetter> nullGetters
    ) {
        this.discriminatorGetter = discriminatorGetter;
        this.nullGetters = nullGetters;
    }
}
