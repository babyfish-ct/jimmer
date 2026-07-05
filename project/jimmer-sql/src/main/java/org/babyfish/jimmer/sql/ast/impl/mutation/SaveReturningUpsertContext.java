package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collections;
import java.util.List;

class SaveReturningUpsertContext implements Dialect.UpsertContext {

    private final SaveReturning returning;

    private final SqlBuilder builder;

    private final EntityCollection<DraftSpi> entities;

    SaveReturningUpsertContext(SaveReturning returning, SqlBuilder builder, EntityCollection<DraftSpi> entities) {
        this.returning = returning;
        this.builder = builder;
        this.entities = entities;
    }

    @Override
    public boolean hasUpdatedColumns() {
        return !returning.upsert.ignoreUpdate &&
                (!returning.updatedGetters.isEmpty() ||
                        (returning.upsert.updateDiscriminator && returning.upsert.discriminatorGetter != null) ||
                        !returning.upsert.nullGetters.isEmpty());
    }

    @Override
    public boolean hasUpdateCondition() {
        return returning.updateCondition != null ||
                returning.upsert.discriminatorGuardGetter != null;
    }

    @Override
    public boolean hasGeneratedId() {
        return returning.upsert.generatedIdProp != null;
    }

    @Override
    public boolean isFakeUpdateRequired() {
        return returning.upsert.fakeUpdate || returning.upsert.generatedIdProp != null;
    }

    @Override
    public boolean isUpdateIgnored() {
        return returning.upsert.ignoreUpdate;
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public boolean isIdInteger() {
        return returning.upsert.generatedIdProp != null &&
                Classes.INT_TYPES.contains(returning.upsert.generatedIdProp.getReturnClass());
    }

    @Override
    public boolean hasConflictPredicate() {
        return returning.upsert.conflictPredicate != null;
    }

    @Override
    public boolean isCurrentRowReturningRequired() {
        return true;
    }

    @Override
    public List<ValueGetter> getConflictGetters() {
        return Collections.unmodifiableList(returning.upsert.conflictGetters);
    }

    @Override
    public Dialect.UpsertContext sql(String sql) {
        builder.sql(sql);
        return this;
    }

    @Override
    public Dialect.UpsertContext sql(ValueGetter getter) {
        builder.sql(getter);
        return this;
    }

    @Override
    public Dialect.UpsertContext enter(AbstractSqlBuilder.ScopeType type) {
        builder.enter(type);
        return this;
    }

    @Override
    public Dialect.UpsertContext separator() {
        builder.separator();
        return this;
    }

    @Override
    public Dialect.UpsertContext leave() {
        builder.leave();
        return this;
    }

    private String tableName() {
        return returning.tableType.getTableName(returning.ctx.options.getSqlClient().getMetadataStrategy());
    }

    @Override
    public Dialect.UpsertContext appendTableName() {
        builder.sql(tableName());
        return this;
    }

    @Override
    public Dialect.UpsertContext appendUpdateConditionWithTableName(
            String sourcePrefix,
            String sourceSuffix
    ) {
        return appendUpdateCondition(tableName() + ".", "", sourcePrefix, sourceSuffix);
    }

    @Override
    public Dialect.UpsertContext appendInsertedColumns(String prefix) {
        for (SaveReturningColumnValue sourceValue : returning.sourceValues) {
            builder.separator().sql(prefix).sql(sourceValue.getter);
        }
        return this;
    }

    @Override
    public Dialect.UpsertContext appendConflictColumns() {
        for (PropertyGetter getter : returning.upsert.conflictGetters) {
            builder.separator().sql(getter);
        }
        return this;
    }

    @Override
    public Dialect.UpsertContext appendConflictPredicate(String alias) {
        if (returning.upsert.conflictPredicate != null) {
            builder.logicalDeleteConflictPredicate(returning.upsert.conflictPredicate, alias);
        }
        return this;
    }

    @Override
    public Dialect.UpsertContext appendInsertingValues() {
        SaveReturningSql.appendSourceValues(returning, builder, entities.iterator().next());
        return this;
    }

    @Override
    public Dialect.UpsertContext appendInsertingRows() {
        SaveReturningSql.appendSourceTuples(returning, builder, entities);
        return this;
    }

    @Override
    public Dialect.UpsertContext appendUpdatingAssignments(String prefix, String suffix) {
        if (returning.upsert.updateDiscriminator && returning.upsert.discriminatorGetter != null) {
            builder.separator()
                    .sql(returning.upsert.discriminatorGetter)
                    .sql(" = ")
                    .sql(prefix)
                    .sql(returning.upsert.discriminatorGetter)
                    .sql(suffix);
        }
        for (PropertyGetter getter : returning.upsert.nullGetters) {
            builder.separator()
                    .sql(getter)
                    .sql(" = null");
        }
        for (PropertyGetter getter : returning.updatedGetters) {
            builder.separator()
                    .sql(getter)
                    .sql(" = ");
            appendUpdatingValue(getter, prefix, suffix);
        }
        return this;
    }

    @Override
    public Dialect.UpsertContext appendConditionalUpdatingAssignments(
            String sourcePrefix,
            String sourceSuffix,
            String valuePrefix,
            String valueSuffix
    ) {
        if (returning.upsert.updateDiscriminator && returning.upsert.discriminatorGetter != null) {
            appendConditionalUpdatingAssignment(returning.upsert.discriminatorGetter, sourcePrefix, sourceSuffix, () -> {
                builder.sql(valuePrefix).sql(returning.upsert.discriminatorGetter).sql(valueSuffix);
            });
        }
        for (PropertyGetter getter : returning.upsert.nullGetters) {
            appendConditionalUpdatingAssignment(getter, sourcePrefix, sourceSuffix, () -> {
                builder.sql("null");
            });
        }
        for (PropertyGetter getter : returning.updatedGetters) {
            appendConditionalUpdatingAssignment(getter, sourcePrefix, sourceSuffix, () -> {
                appendUpdatingValue(getter, valuePrefix, valueSuffix);
            });
        }
        return this;
    }

    @Override
    public Dialect.UpsertContext appendUpdateCondition(
            String targetPrefix,
            String targetSuffix,
            String sourcePrefix,
            String sourceSuffix
    ) {
        boolean hasPrevious = false;
        if (returning.updateCondition != null) {
            returning.updateCondition.append(builder, targetPrefix, targetSuffix, sourcePrefix, sourceSuffix);
            hasPrevious = true;
        }
        if (returning.upsert.discriminatorGuardGetter != null) {
            if (hasPrevious) {
                builder.sql(" and ");
            }
            builder
                    .sql(targetPrefix)
                    .sql(returning.upsert.discriminatorGuardGetter)
                    .sql(targetSuffix)
                    .sql(" = ")
                    .sql(sourcePrefix)
                    .sql(returning.upsert.discriminatorGuardGetter)
                    .sql(sourceSuffix);
        }
        return this;
    }

    @Override
    public Dialect.UpsertContext appendGeneratedId() {
        if (returning.idGetter != null) {
            builder.sql(returning.idGetter);
        }
        return this;
    }

    @Override
    public Dialect.UpsertContext appendReturning(String prefix) {
        SaveReturningSql.appendReturning(returning, builder, prefix);
        return this;
    }

    @Override
    public Dialect.UpsertContext appendId() {
        if (returning.idGetter != null) {
            builder.sql(returning.idGetter);
        }
        return this;
    }

    private void appendConditionalUpdatingAssignment(
            PropertyGetter getter,
            String sourcePrefix,
            String sourceSuffix,
            Runnable valueAppender
    ) {
        builder.separator()
                .sql(getter)
                .sql(" = if(");
        appendUpdateCondition("", "", sourcePrefix, sourceSuffix);
        builder.sql(", ");
        valueAppender.run();
        builder.sql(", ").sql(getter).sql(")");
    }

    private void appendUpdatingValue(PropertyGetter getter, String prefix, String suffix) {
        builder.sql(prefix)
                .sql(getter)
                .sql(suffix);
    }
}
