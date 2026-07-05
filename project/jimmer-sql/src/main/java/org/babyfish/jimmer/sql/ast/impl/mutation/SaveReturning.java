package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

class SaveReturning {

    final SaveContext ctx;

    final Shape shape;

    final ImmutableType tableType;

    final SaveReturningKind kind;

    final SaveReturningMatchMode matchMode;

    final List<SaveReturningColumnValue> sourceValues;

    final List<PropertyGetter> updatedGetters;

    @Nullable
    final SaveReturningUpdateCondition updateCondition;

    @Nullable
    final PropertyGetter versionGetter;

    @Nullable
    final PropertyGetter idGetter;

    final List<PropertyGetter> matchGetters;

    final List<Integer> matchIndexes;

    final List<PropertyGetter> returningGetters;

    final List<ImmutableProp> returningProps;

    final int logicalDeletedIndex;

    @Nullable
    final LogicalDeletedInfo logicalDeletedInfo;

    @Nullable
    final SaveReturningUpsert upsert;

    SaveReturning(
            SaveContext ctx,
            Shape shape,
            ImmutableType tableType,
            SaveReturningKind kind,
            SaveReturningMatchMode matchMode,
            List<SaveReturningColumnValue> sourceValues,
            List<PropertyGetter> updatedGetters,
            @Nullable SaveReturningUpdateCondition updateCondition,
            @Nullable PropertyGetter versionGetter,
            @Nullable PropertyGetter idGetter,
            List<PropertyGetter> matchGetters,
            List<Integer> matchIndexes,
            List<PropertyGetter> returningGetters,
            List<ImmutableProp> returningProps,
            int logicalDeletedIndex,
            @Nullable LogicalDeletedInfo logicalDeletedInfo,
            @Nullable SaveReturningUpsert upsert
    ) {
        this.ctx = ctx;
        this.shape = shape;
        this.tableType = tableType;
        this.kind = kind;
        this.matchMode = matchMode;
        this.sourceValues = sourceValues;
        this.updatedGetters = updatedGetters;
        this.updateCondition = updateCondition;
        this.versionGetter = versionGetter;
        this.idGetter = idGetter;
        this.matchGetters = matchGetters;
        this.matchIndexes = matchIndexes;
        this.returningGetters = returningGetters;
        this.returningProps = returningProps;
        this.logicalDeletedIndex = logicalDeletedIndex;
        this.logicalDeletedInfo = logicalDeletedInfo;
        this.upsert = upsert;
    }

    static @Nullable SaveReturning forInsert(
            SaveContext ctx,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            ImmutableType tableType,
            @Nullable SequenceIdGenerator sequenceIdGenerator,
            boolean generatedId,
            List<PropertyGetter> insertedGetters,
            @Nullable PropertyGetter discriminatorGetter,
            List<PropertyGetter> defaultGetters
    ) {
        return SaveReturningFactory.forInsert(
                ctx,
                shape,
                entities,
                tableType,
                sequenceIdGenerator,
                generatedId,
                insertedGetters,
                discriminatorGetter,
                defaultGetters
        );
    }

    static @Nullable SaveReturning forUpdate(
            SaveContext ctx,
            Shape shape,
            EntityCollection<DraftSpi> entities,
            List<PropertyGetter> updatedGetters,
            @Nullable ImmutableProp discriminatorProp,
            @Nullable ImmutableProp discriminatorGuardProp,
            @Nullable Object discriminatorGuardValue,
            List<PropertyGetter> nullGetters,
            @Nullable SaveReturningUpdateCondition updateCondition,
            @Nullable Set<ImmutableProp> keyProps,
            @Nullable Predicate userOptimisticLockPredicate,
            @Nullable PropertyGetter versionGetter,
            boolean fakeUpdate,
            boolean forceOneByOne
    ) {
        return SaveReturningFactory.forUpdate(
                ctx,
                shape,
                entities,
                updatedGetters,
                discriminatorProp,
                discriminatorGuardProp,
                discriminatorGuardValue,
                nullGetters,
                updateCondition,
                keyProps,
                userOptimisticLockPredicate,
                versionGetter,
                fakeUpdate,
                forceOneByOne
        );
    }

    static @Nullable SaveReturning forUpsert(
            SaveContext ctx,
            Batch<DraftSpi> batch,
            ImmutableType tableType,
            @Nullable ImmutableProp generatedIdProp,
            @Nullable SequenceIdGenerator sequenceIdGenerator,
            List<PropertyGetter> insertedGetters,
            @Nullable ImmutableProp discriminatorProp,
            boolean updateDiscriminator,
            @Nullable ImmutableProp discriminatorGuardProp,
            List<PropertyGetter> nullGetters,
            List<PropertyGetter> conflictGetters,
            @Nullable LogicalDeletedInfo conflictPredicate,
            List<PropertyGetter> updatedGetters,
            boolean ignoreUpdate,
            @Nullable Predicate userOptimisticLockPredicate,
            @Nullable PropertyGetter versionGetter,
            boolean fakeUpdate,
            boolean forceOneByOne
    ) {
        return SaveReturningFactory.forUpsert(
                ctx,
                batch,
                tableType,
                generatedIdProp,
                sequenceIdGenerator,
                insertedGetters,
                discriminatorProp,
                updateDiscriminator,
                discriminatorGuardProp,
                nullGetters,
                conflictGetters,
                conflictPredicate,
                updatedGetters,
                ignoreUpdate,
                userOptimisticLockPredicate,
                versionGetter,
                fakeUpdate,
                forceOneByOne
        );
    }

    int[] executeInsert(EntityCollection<DraftSpi> entities) {
        return SaveReturningExecutor.executeInsert(this, entities);
    }

    int[] executeUpdate(EntityCollection<DraftSpi> entities) {
        return SaveReturningExecutor.executeUpdate(this, entities);
    }

    int[] executeUpsert(EntityCollection<DraftSpi> entities) {
        return SaveReturningExecutor.executeUpsert(this, entities);
    }
}
