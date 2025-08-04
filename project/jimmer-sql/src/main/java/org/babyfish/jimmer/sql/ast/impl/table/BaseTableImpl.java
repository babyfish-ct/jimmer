package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.base.AbstractBaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BaseTableImpl extends AbstractDataManager<BaseTableImpl.Key, BaseTableImpl> implements BaseTableImplementor {

    private final BaseTableSymbol symbol;

    private final TableLikeImplementor<?> parent;

    private final BaseTableImplementor recursive;

    private BaseTableSymbol rootSymbol;

    // Only uses when parent is null
    private RealTable rootRealTable;

    public static BaseTableImplementor of(
            BaseTableSymbol symbol,
            TableLikeImplementor<?> parent,
            BaseTableImplementor recursive
    ) {
        if (parent == null) {
            return new BaseTableImpl(symbol, null, null);
        }
        if (parent instanceof TableImplementor<?>) {
            ((TableImplementor<?>) parent).setHasBaseTable();
        }
        BaseTableImpl child;
        if (parent instanceof BaseTableImplementor) {
            BaseTableImpl parentImpl = (BaseTableImpl) parent;
            Key key = new Key(symbol.getWeakJoinHandle(), symbol.getJoinType());
            child = parentImpl.getValue(key);
            if (child == null) {
                child = new BaseTableImpl(symbol, parentImpl, recursive);
                parentImpl.putValue(key, child);
            }
        } else {
            TableImpl<?> parentImpl = (TableImpl<?>) parent;
            parentImpl.setHasBaseTable();
            child = parentImpl.computedIfAbsent(
                    new TableImpl.Key("", symbol.getJoinType(), symbol.getWeakJoinHandle(), false),
                    () -> new BaseTableImpl(symbol, parent, recursive)
            );
        }
        return child;
    }

    private BaseTableImpl(BaseTableSymbol symbol, TableLikeImplementor<?> parent, BaseTableImplementor recursive) {
        this.symbol = symbol;
        this.parent = parent;
        this.recursive = recursive;
    }

    @Override
    public TableLikeImplementor<?> getParent() {
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
    public BaseTableImplementor getRecursive() {
        return recursive;
    }

    @Override
    public boolean isCte() {
        return toSymbol().isCte();
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
        visitor.visitTableReference(realTable(visitor.getAstContext()), null, false);
        actualQuery(symbol).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql(" from ");
        realTable(builder.assertSimple().getAstContext()).renderTo(builder, false);
    }

    void renderBaseQueryCore(AbstractSqlBuilder<?> builder) {
        actualQuery(symbol).renderTo(builder);
    }

    private static TypedBaseQueryImplementor<?> actualQuery(BaseTableSymbol symbol) {
        ConfigurableBaseQueryImpl<?> query = symbol.getQuery();
        MergedBaseQueryImpl<?> mergedBy = query.getMergedBy();
        return mergedBy != null ? mergedBy : query;
    }

    @Override
    public AbstractMutableStatementImpl getStatement() {
        return getRootSymbol().getQuery().getMutableQuery();
    }

    private BaseTableSymbol getRootSymbol() {
        BaseTableSymbol rs = rootSymbol;
        if (rs == null) {
            rootSymbol = rs = createRootSymbol();
        }
        return rs;
    }

    private BaseTableSymbol createRootSymbol() {
        if (parent instanceof BaseTableImpl) {
            return ((BaseTableImpl) parent).createRootSymbol();
        }
        return symbol;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BaseTableImpl");
        toString(true, true, builder);
        return builder.toString();
    }

    private void toString(boolean up, boolean down, StringBuilder builder) {
        builder.append("{");
        builder.append("symbol=").append(symbol);
        if (up && parent != null) {
            builder.append(",parent=");
            if (parent instanceof BaseTableImpl) {
                ((BaseTableImpl)parent).toString(true, false, builder);
            } else {
                builder.append(parent);
            }
        }
        if (down && !isEmpty()) {
            builder.append(",children=[");
            boolean addComma = false;
            for (BaseTableImpl child : this) {
                if (addComma) {
                    builder.append(",");
                } else {
                    addComma = true;
                }
                child.toString(false, true, builder);
            }
            builder.append(']');
        }
        builder.append("}");
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

    @Override
    public boolean hasBaseTable() {
        return true;
    }
}
