package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractMutableStatementImpl implements Filterable {

    private final TableAliasAllocator tableAliasAllocator;

    private final JSqlClient sqlClient;

    private boolean frozen;

    private List<Predicate> predicates = new ArrayList<>();

    public AbstractMutableStatementImpl(
            TableAliasAllocator tableAliasAllocator,
            JSqlClient sqlClient
    ) {
        this.tableAliasAllocator = tableAliasAllocator;
        if (!(this instanceof Fake)) {
            Objects.requireNonNull(sqlClient, "sqlClient cannot be null");
            this.sqlClient = sqlClient;
        } else {
            this.sqlClient = null;
        }
    }

    public abstract <T extends Table<?>> T getTable();

    public Predicate getPredicate() {
        return predicates.isEmpty() ? null : predicates.get(0);
    }

    public final boolean freeze() {
        if (frozen) {
            return false;
        }
        onFrozen();
        frozen = true;
        return true;
    }

    protected void onFrozen() {
        if (predicates.size() > 1) {
            predicates = mergePredicates(predicates);
        }
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

    public JSqlClient getSqlClient() {
        JSqlClient client = sqlClient;
        if (client == null) {
            throw new UnsupportedOperationException(
                    "getSqlClient() is not supported by " + Fake.class.getName()
            );
        }
        return client;
    }

    @Override
    public Filterable where(Predicate ... predicates) {
        validateMutable();
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                this.predicates.add(predicate);
            }
        }
        return this;
    }

    @Override
    public <T extends Table<?>, R> ConfigurableSubQuery<R> createSubQuery(
            Class<T> tableType, BiFunction<MutableSubQuery, T, ConfigurableSubQuery<R>> block
    ) {
        return Queries.createSubQuery(this, tableType, block);
    }

    @Override
    public <T extends Table<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType, BiConsumer<MutableSubQuery, T> block
    ) {
        return Queries.createWildSubQuery(this, tableType, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    ConfigurableSubQuery<R> createAssociationSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>, ConfigurableSubQuery<R>> block
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

    private static final Predicate[] EMPTY_PREDICATE = new Predicate[0];

    protected static List<Predicate> mergePredicates(List<Predicate> predicates) {
        if (predicates.size() < 2) {
            return predicates;
        }
        return Collections.singletonList(
                Predicate.and(
                        predicates.toArray(EMPTY_PREDICATE)
                )
        );
    }

    private static class Fake extends AbstractMutableStatementImpl {

        private Fake() {
            super(new TableAliasAllocator(), null);
        }

        @Override
        public AbstractMutableStatementImpl where(Predicate ... predicates) {
            throw new UnsupportedOperationException("Fake statement does not support where operation");
        }

        @Override
        public <T extends Table<?>> T getTable() {
            throw new UnsupportedOperationException("Fake statement does not support table property");
        }
    }
}
