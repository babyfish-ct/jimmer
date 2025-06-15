package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.base.MergedBaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BaseTableImpl extends AbstractDataManager<BaseTableImpl.Key, BaseTableImpl> implements BaseTableImplementor {

    private final BaseTableSymbol symbol;

    private final BaseTableImpl parent;

    // Only uses when parent is null
    private RealTable rootRealTable;

    public static BaseTableImplementor of(BaseTableSymbol symbol, BaseTableImpl parent) {
        if (parent == null) {
            return new BaseTableImpl(symbol, null);
        }
        Key key = new Key(symbol.getWeakJoinHandle(), symbol.getJoinType());
        BaseTableImpl child = parent.getValue(key);
        if (child == null) {
            child = new BaseTableImpl(symbol, parent);
            parent.putValue(key, child);
        }
        return child;
    }

    private BaseTableImpl(BaseTableSymbol symbol, BaseTableImpl parent) {
        this.symbol = symbol;
        this.parent = parent;
    }

    @Override
    public BaseTableImplementor getParent() {
        return parent;
    }

    @Override
    public WeakJoinHandle getWeakJoinHandle() {
        return symbol.getWeakJoinHandle();
    }

    @Override
    public List<Selection<?>> getSelections() {
        return symbol.getSelections();
    }

    @Override
    public JoinType getJoinType() {
        return symbol.getJoinType();
    }

    @Override
    public TypedBaseQueryImplementor<?> getQuery() {
        return symbol.getQuery();
    }

    @Override
    public BaseTableSymbol toSymbol() {
        return symbol;
    }

    @Override
    public RealTable realTable(JoinTypeMergeScope scope) {
        if (parent == null) {
            RealTable rrt = this.rootRealTable;
            if (rrt == null) {
                this.rootRealTable = rrt = new RealTableImpl(this);
            }
            return rrt;
        }
        RealTableImpl parentRealTable = (RealTableImpl) parent.realTable(scope);
        return parentRealTable.child(scope, this);
    }

    @Override
    public void accept(AstVisitor visitor) {
        actualQuery(symbol).accept(visitor);
        for (BaseTableImpl subTable : this) {
            actualQuery(subTable.symbol).accept(visitor);
        }
        visitor.visitTableReference(realTable(visitor.getAstContext().getJoinTypeMergeScope()), null, false);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql(" from ").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
        realTable(builder.assertSimple().getAstContext().getJoinTypeMergeScope()).renderTo(builder);
        builder.leave().sql(" ").sql(realTable(builder.assertSimple().getAstContext().getJoinTypeMergeScope()).getAlias());
    }

    void renderBaseQuery(AbstractSqlBuilder<?> builder) {
        actualQuery(symbol).renderTo(builder);
    }

    private static TypedBaseQueryImplementor<?> actualQuery(BaseTableSymbol symbol) {
        ConfigurableBaseQueryImpl<?> query = symbol.getQuery();
        MergedBaseQueryImpl<?> mergedBy = query.getMergedBy();
        return mergedBy != null ? mergedBy : query;
    }

    @Override
    public AbstractMutableStatementImpl getStatement() {
        return symbol.getQuery().getMutableQuery();
    }

    public static class Key {

        final WeakJoinHandle handle;

        final JoinType joinType;

        Key(WeakJoinHandle handle, JoinType joinType) {
            this.handle = handle;
            this.joinType = joinType;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;
            return handle.equals(key.handle) && joinType == key.joinType;
        }

        @Override
        public int hashCode() {
            int result = handle.hashCode();
            result = 31 * result + joinType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "handle=" + handle +
                    ", joinType=" + joinType +
                    '}';
        }
    }
}
