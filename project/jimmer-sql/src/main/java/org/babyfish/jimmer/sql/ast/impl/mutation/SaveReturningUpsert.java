package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class SaveReturningUpsert {

    @Nullable
    final ImmutableProp generatedIdProp;

    @Nullable
    final SequenceIdGenerator sequenceIdGenerator;

    @Nullable
    final PropertyGetter discriminatorGetter;

    final boolean updateDiscriminator;

    @Nullable
    final PropertyGetter discriminatorGuardGetter;

    final List<PropertyGetter> nullGetters;

    final List<PropertyGetter> conflictGetters;

    @Nullable
    final LogicalDeletedInfo conflictPredicate;

    final boolean ignoreUpdate;

    final boolean fakeUpdate;

    SaveReturningUpsert(
            @Nullable ImmutableProp generatedIdProp,
            @Nullable SequenceIdGenerator sequenceIdGenerator,
            @Nullable PropertyGetter discriminatorGetter,
            boolean updateDiscriminator,
            @Nullable PropertyGetter discriminatorGuardGetter,
            List<PropertyGetter> nullGetters,
            List<PropertyGetter> conflictGetters,
            @Nullable LogicalDeletedInfo conflictPredicate,
            boolean ignoreUpdate,
            boolean fakeUpdate
    ) {
        this.generatedIdProp = generatedIdProp;
        this.sequenceIdGenerator = sequenceIdGenerator;
        this.discriminatorGetter = discriminatorGetter;
        this.updateDiscriminator = updateDiscriminator;
        this.discriminatorGuardGetter = discriminatorGuardGetter;
        this.nullGetters = nullGetters;
        this.conflictGetters = conflictGetters;
        this.conflictPredicate = conflictPredicate;
        this.ignoreUpdate = ignoreUpdate;
        this.fakeUpdate = fakeUpdate;
    }
}
