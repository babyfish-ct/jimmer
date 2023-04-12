package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.FieldFilterArgs;
import org.babyfish.jimmer.sql.fetcher.spi.FieldFilterArgsImplementor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

class MiddleEntityJoinFieldFilter implements FieldFilter<Table<?>> {

    @NotNull
    private final FieldFilter<Table<?>> deeperFilter;

    @NotNull
    private final String deeperPropName;

    MiddleEntityJoinFieldFilter(
            @NotNull FieldFilter<Table<?>> targetFilter,
            @NotNull String deeperPropName) {
        this.deeperFilter = targetFilter;
        this.deeperPropName = deeperPropName;
    }

    @Override
    public void apply(FieldFilterArgs<Table<?>> args) {
        deeperFilter.apply(new Args(args, deeperPropName));
    }

    private static class Args implements FieldFilterArgsImplementor<Table<?>> {

        private final FieldFilterArgs<?> raw;

        private final String deeperPropName;

        Args(FieldFilterArgs<?> raw, String deeperPropName) {
            this.raw = raw;
            this.deeperPropName = deeperPropName;
        }

        @Override
        public Table<?> getTable() {
            return raw.getTable().join(deeperPropName);
        }

        @Override
        @OldChain
        public Sortable where(Predicate... predicates) {
            return raw.where(predicates);
        }

        @Override
        @OldChain
        public Sortable orderBy(Expression<?>... expressions) {
            return raw.orderBy(expressions);
        }

        @Override
        @OldChain
        public Sortable orderBy(Order... orders) {
            return raw.orderBy(orders);
        }

        @Override
        @OldChain
        public Sortable orderBy(List<Order> orders) {
            return raw.orderBy(orders);
        }

        @Override
        public MutableSubQuery createSubQuery(TableProxy<?> table) {
            return raw.createSubQuery(table);
        }

        @Override
        public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>> MutableSubQuery createAssociationSubQuery(AssociationTable<SE, ST, TE, TT> table) {
            return raw.createAssociationSubQuery(table);
        }

        @Override
        public <K> K getKey() {
            return raw.getKey();
        }

        @Override
        public <K> Collection<K> getKeys() {
            return raw.getKeys();
        }

        @Override
        public AbstractMutableQueryImpl query() {
            return ((FieldFilterArgsImplementor<?>)raw).query();
        }
    }
}
