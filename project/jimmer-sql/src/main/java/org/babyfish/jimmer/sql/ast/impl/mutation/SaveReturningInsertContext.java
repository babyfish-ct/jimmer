package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

class SaveReturningInsertContext implements Dialect.InsertReturningContext {

    private final SaveReturning returning;

    private final SqlBuilder builder;

    private final EntityCollection<DraftSpi> entities;

    SaveReturningInsertContext(SaveReturning returning, SqlBuilder builder, EntityCollection<DraftSpi> entities) {
        this.returning = returning;
        this.builder = builder;
        this.entities = entities;
    }

    @Override
    public Dialect.InsertReturningContext sql(String sql) {
        builder.sql(sql);
        return this;
    }

    @Override
    public Dialect.InsertReturningContext enter(AbstractSqlBuilder.ScopeType type) {
        builder.enter(type);
        return this;
    }

    @Override
    public Dialect.InsertReturningContext separator() {
        builder.separator();
        return this;
    }

    @Override
    public Dialect.InsertReturningContext leave() {
        builder.leave();
        return this;
    }

    @Override
    public Dialect.InsertReturningContext appendTableName() {
        builder.sql(returning.tableType.getTableName(returning.ctx.options.getSqlClient().getMetadataStrategy()));
        return this;
    }

    @Override
    public Dialect.InsertReturningContext appendInsertedColumns() {
        SaveReturningSql.appendSourceColumns(returning, builder);
        return this;
    }

    @Override
    public Dialect.InsertReturningContext appendInsertingValues() {
        SaveReturningSql.appendSourceTuples(returning, builder, entities);
        return this;
    }

    @Override
    public Dialect.InsertReturningContext appendReturning(String prefix) {
        SaveReturningSql.appendReturning(returning, builder, prefix);
        return this;
    }
}
