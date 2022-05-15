package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableExImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import java.util.function.Function;

public class ImmutableProps {

    private ImmutableProps() {}

    public static <T extends Table<?>> ImmutableProp get(
            Class<T> tableType,
            Function<T, PropExpression<?>> block
    ) {
        TableImplementor<?> tableImpl = TableImplementor.create(
                AbstractMutableStatementImpl.fake(),
                ImmutableType.get(tableType)
        );
        T table = TableWrappers.wrap(tableImpl);
        PropExpressionImpl<?> propExpr = (PropExpressionImpl<?>) block.apply(table);
        if (propExpr == null) {
            throw new IllegalStateException(
                    "The lambda expression of ImmutableProps.get cannot return null"
            );
        }
        if (propExpr.getTableImplementor() != tableImpl) {
            throw new IllegalStateException(
                    "The lambda expression of ImmutableProps.get must return an expression bases on its argument"
            );
        }
        return propExpr.getProp();
    }

    public static <T extends Table<?>> ImmutableProp join(
            Class<T> tableType,
            Function<T, Table<?>> block
    ) {
        TableImplementor<?> tableImpl;
        if (TableEx.class.isAssignableFrom(tableType)) {
            tableImpl = TableExImplementor.create(
                    AbstractMutableStatementImpl.fake(),
                    ImmutableType.get(tableType)
            );
        } else {
            tableImpl = TableImplementor.create(
                    AbstractMutableStatementImpl.fake(),
                    ImmutableType.get(tableType)
            );
        }
        T table = TableWrappers.wrap(tableImpl);
        Table<?> joinedTable = block.apply(table);
        if (joinedTable == null) {
            throw new IllegalStateException(
                    "The lambda expression of ImmutableProps.join cannot return null"
            );
        }
        TableImplementor<?> joinedTableImpl = TableImplementor.unwrap(joinedTable);
        if (joinedTableImpl.getParent() != tableImpl) {
            throw new IllegalStateException(
                    "The lambda expression of ImmutableProps.join must return an child table bases on its argument"
            );
        }
        if (joinedTableImpl.isInverse()) {
            ImmutableProp mappedBy = joinedTableImpl.getJoinProp().getOpposite();
            if (mappedBy == null) {
                throw new AssertionError("Internal bug: Cannot get opposite property for inverse join");
            }
            return mappedBy;
        }
        return joinedTableImpl.getJoinProp();
    }
}
