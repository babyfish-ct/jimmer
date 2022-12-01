package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

import java.util.function.Function;

public class ImmutableProps {

    private ImmutableProps() {}

    @SuppressWarnings("unchecked")
    public static <T extends Table<?>> ImmutableProp get(
            Class<T> tableType,
            Function<T, PropExpression<?>> block
    ) {
        TableImplementor<?> tableImpl = TableImplementor.create(
                null,
                ImmutableType.get(tableType)
        );
        T table = TableProxies.wrap(tableImpl);
        PropExpressionImpl<?> propExpr = (PropExpressionImpl<?>) block.apply(table);
        if (propExpr == null) {
            throw new IllegalStateException(
                    "The lambda expression of ImmutableProps.get cannot return null"
            );
        }
        if (propExpr.getTable() != tableImpl) {
            throw new IllegalStateException(
                    "The lambda expression of ImmutableProps.get must return an expression bases on its argument"
            );
        }
        return propExpr.getProp();
    }

    @SuppressWarnings("unchecked")
    public static <ST extends Table<?>> ImmutableProp join(
            Class<ST> sourceTableType,
            Function<ST, ? extends Table<?>> block
    ) {
        TableImplementor<?> tableImpl = TableImplementor.create(
                new FakeStatement(ImmutableType.get(sourceTableType)),
                ImmutableType.get(sourceTableType)
        );
        ST table = TableProxies.wrap(tableImpl);
        Table<?> joinedTable = block.apply(table);
        if (joinedTable == null) {
            throw new IllegalStateException(
                    "The lambda expression of ImmutableProps.join cannot return null"
            );
        }
        TableImplementor<?> joinedTableImpl = TableProxies.resolve(joinedTable, null);
        if (joinedTableImpl.getParent() != tableImpl) {
            throw new IllegalStateException(
                    "The lambda expression of ImmutableProps.join must return an child table bases on its argument"
            );
        }
        if (joinedTableImpl.isInverse()) {
            ImmutableProp opposite = joinedTableImpl.getJoinProp().getOpposite();
            if (opposite == null) {
                throw new AssertionError("Internal bug: Cannot get opposite property for inverse join");
            }
            return opposite;
        }
        ImmutableProp joinProp = joinedTableImpl.getJoinProp();
        if (joinProp == null) {
            throw new IllegalArgumentException("The lambda does not returns table base on association property");
        }
        return joinProp;
    }

    private static class FakeStatement extends AbstractMutableStatementImpl {

        private StatementContext ctx;

        public FakeStatement(ImmutableType type) {
            super(null, type);
            this.ctx = new StatementContext(ExecutionPurpose.QUERY, false);
        }

        @Override
        public AbstractMutableStatementImpl getParent() {
            return null;
        }

        @Override
        public StatementContext getContext() {
            return ctx;
        }
    }
}
