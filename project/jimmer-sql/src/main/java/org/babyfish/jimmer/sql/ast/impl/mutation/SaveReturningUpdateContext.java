package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

class SaveReturningUpdateContext implements Dialect.UpdateByValuesContext {

    private final SaveReturning returning;

    private final SqlBuilder builder;

    private final EntityCollection<DraftSpi> entities;

    SaveReturningUpdateContext(SaveReturning returning, SqlBuilder builder, EntityCollection<DraftSpi> entities) {
        this.returning = returning;
        this.builder = builder;
        this.entities = entities;
    }

    @Override
    public Dialect.UpdateByValuesContext sql(String sql) {
        builder.sql(sql);
        return this;
    }

    @Override
    public Dialect.UpdateByValuesContext enter(AbstractSqlBuilder.ScopeType type) {
        builder.enter(type);
        return this;
    }

    @Override
    public Dialect.UpdateByValuesContext separator() {
        builder.separator();
        return this;
    }

    @Override
    public Dialect.UpdateByValuesContext leave() {
        builder.leave();
        return this;
    }

    @Override
    public Dialect.UpdateByValuesContext appendTableName() {
        builder.sql(returning.shape.getType().getTableName(returning.ctx.options.getSqlClient().getMetadataStrategy()));
        return this;
    }

    @Override
    public Dialect.UpdateByValuesContext appendSource() {
        builder
                .enter(AbstractSqlBuilder.ScopeType.LIST)
                .enter(AbstractSqlBuilder.ScopeType.VALUES);
        SaveReturningSql.appendSourceTuples(returning, builder, entities);
        builder
                .leave()
                .leave();
        return this;
    }

    @Override
    public Dialect.UpdateByValuesContext appendSourceColumns() {
        SaveReturningSql.appendSourceColumns(returning, builder);
        return this;
    }

    @Override
    public Dialect.UpdateByValuesContext appendAssignments(String targetPrefix, String sourcePrefix) {
        for (PropertyGetter getter : returning.updatedGetters) {
            builder.separator()
                    .sql(getter)
                    .sql(" = ")
                    .sql(sourcePrefix)
                    .sql(getter);
        }
        if (returning.versionGetter != null) {
            builder.separator()
                    .sql(returning.versionGetter)
                    .sql(" = ")
                    .sql(targetPrefix)
                    .sql(returning.versionGetter)
                    .sql(" + 1");
        }
        return this;
    }

    @Override
    public Dialect.UpdateByValuesContext appendPredicates(String targetPrefix, String sourcePrefix) {
        builder
                .sql(targetPrefix)
                .sql(returning.idGetter)
                .sql(" = ")
                .sql(sourcePrefix)
                .sql(returning.idGetter);
        if (returning.versionGetter != null) {
            builder
                    .sql(" and ")
                    .sql(targetPrefix)
                    .sql(returning.versionGetter)
                    .sql(" = ")
                    .sql(sourcePrefix)
                    .sql(returning.versionGetter);
        }
        return this;
    }

    @Override
    public Dialect.UpdateByValuesContext appendReturning(String targetPrefix) {
        SaveReturningSql.appendReturning(returning, builder, targetPrefix);
        return this;
    }
}
