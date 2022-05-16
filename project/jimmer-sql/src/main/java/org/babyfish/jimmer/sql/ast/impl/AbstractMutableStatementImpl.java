package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedSubQuery;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractMutableStatementImpl implements Filterable {

    private TableAliasAllocator tableAliasAllocator;

    private SqlClient sqlClient;

    private boolean frozen;

    public AbstractMutableStatementImpl(
            TableAliasAllocator tableAliasAllocator,
            SqlClient sqlClient
    ) {
        this.tableAliasAllocator = tableAliasAllocator;
        if (!(this instanceof Fake)) {
            Objects.requireNonNull(sqlClient, "sqlClient cannot be null");
            this.sqlClient = sqlClient;
        }
    }

    public void freeze() {
        frozen = true;
    }

    public void validateMutable() {
        if (frozen) {
            throw new IllegalStateException(
                    "Cannot mutate the statement because it has been frozen"
            );
        }
    }

    public TableAliasAllocator getTableAliasAllocator() {
        return tableAliasAllocator;
    }

    public SqlClient getSqlClient() {
        SqlClient client = sqlClient;
        if (client == null) {
            throw new UnsupportedOperationException(
                    "getSqlClient() is not supported by " + Fake.class.getName()
            );
        }
        return client;
    }

    @Override
    public <T extends TableEx<?>, R> ConfigurableTypedSubQuery<R> createSubQuery(
            Class<T> tableType, BiFunction<MutableSubQuery, T, ConfigurableTypedSubQuery<R>> block
    ) {
        return Queries.createSubQuery(this, tableType, block);
    }

    @Override
    public <T extends TableEx<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType, BiConsumer<MutableSubQuery, T> block
    ) {
        return Queries.createWildSubQuery(this, tableType, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    ConfigurableTypedSubQuery<R> createAssociationSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>, ConfigurableTypedSubQuery<R>> block
    ) {
        return Queries.createAssociationSubQuery(this, sourceTableType, targetTableGetter, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    MutableSubQuery createAssociationWildSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiConsumer<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>> block
    ) {
        return Queries.createAssociationWildSubQuery(this, sourceTableType, targetTableGetter, block);
    }

    public static AbstractMutableStatementImpl fake() {
        return new Fake();
    }

    private static class Fake extends AbstractMutableStatementImpl {

        private Fake() {
            super(new TableAliasAllocator(), null);
        }

        @Override
        public Filterable where(Predicate ... predicates) {
            throw new UnsupportedOperationException("Fake statement does not support where operation");
        }
    }
}
