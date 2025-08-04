package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.MutableRecursiveBaseQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class MutableRecursiveBaseQueryImpl<R extends BaseTable>
        extends MutableBaseQueryImpl implements MutableRecursiveBaseQuery<R> {

    private final R recursive;

    public MutableRecursiveBaseQueryImpl(JSqlClientImplementor sqlClient, TableProxy<?> table, R recursive) {
        super(sqlClient, table);
        this.recursive = recursive;
    }

    public MutableRecursiveBaseQueryImpl(
            JSqlClientImplementor sqlClient,
            ImmutableType type,
            Function<TableImplementor<?>, R> recursiveCreator) {
        super(sqlClient, type);
        this.recursive = recursiveCreator.apply((TableImplementor<?>) getTableLikeImplementor());
    }

    @Override
    public R recursive() {
        return recursive;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> where(Predicate... predicates) {
        return (MutableRecursiveBaseQueryImpl<R>)super.where(predicates);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> whereIf(boolean condition, Supplier<Predicate> block) {
        return (MutableRecursiveBaseQueryImpl<R>) super.whereIf(condition, block);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> whereIf(boolean condition, Predicate predicate) {
        return (MutableRecursiveBaseQueryImpl<R>)super.whereIf(condition, predicate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> orderBy(Expression<?>... expressions) {
        return (MutableRecursiveBaseQueryImpl<R>)super.orderBy(expressions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> orderBy(List<Order> orders) {
        return (MutableRecursiveBaseQueryImpl<R>)super.orderBy(orders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> orderBy(Order... orders) {
        return (MutableRecursiveBaseQueryImpl<R>)super.orderBy(orders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> orderByIf(boolean condition, Expression<?>... expressions) {
        return (MutableRecursiveBaseQueryImpl<R>)super.orderByIf(condition, expressions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> orderByIf(boolean condition, List<Order> orders) {
        return (MutableRecursiveBaseQueryImpl<R>)super.orderByIf(condition, orders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> orderByIf(boolean condition, Order... orders) {
        return (MutableRecursiveBaseQueryImpl<R>)super.orderByIf(condition, orders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> groupBy(Expression<?>... expressions) {
        return (MutableRecursiveBaseQueryImpl<R>)super.groupBy(expressions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRecursiveBaseQueryImpl<R> having(Predicate... predicates) {
        return (MutableRecursiveBaseQueryImpl<R>)super.having(predicates);
    }

    @Override
    void accept(
            AstVisitor visitor,
            List<Selection<?>> overriddenSelections,
            boolean withoutSortingAndPaging
    ) {
        super.accept(visitor, overriddenSelections, withoutSortingAndPaging);
        AstContext astContext = visitor.getAstContext();
        BaseTableImplementor implementor = astContext.resolveBaseTable((BaseTableSymbol) recursive);
        visitor.visitTableReference(
                implementor.realTable(visitor.getAstContext().getJoinTypeMergeScope()),
                null,
                false
        );
    }
}
