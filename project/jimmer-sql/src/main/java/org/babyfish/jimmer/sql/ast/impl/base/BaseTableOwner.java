package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.table.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.jetbrains.annotations.Nullable;

public final class BaseTableOwner {

    final BaseTableImplementor<?> baeTable;

    final int index;

    final String path;

    final BaseTableOwner parent;

    final RealTable.Key childKey;

    public BaseTableOwner(BaseTableImplementor<?> baeTable, int index) {
        this.parent = null;
        this.baeTable = baeTable;
        this.index = index;
        this.childKey = null;
        this.path = Integer.toString(index);
    }

    public BaseTableOwner(BaseTableOwner parent, RealTable.Key childKey) {
        this.parent = parent;
        this.baeTable = parent.baeTable;
        this.index = parent.index;
        this.childKey = childKey;
        this.path = parent.path + '/' + childKey;
    }

    public BaseTableImplementor<?> getBaseTable() {
        return baeTable;
    }

    public int getIndex() {
        return index;
    }

    public String getPath() {
        return path;
    }

    @Nullable
    public BaseTableOwner getParent() {
        return parent;
    }

    public RealTable.Key getChildKey() {
        return childKey;
    }

    public BaseTableOwner sub(RealTable.Key childKey) {
        return new BaseTableOwner(this, childKey);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BaseTableOwner that = (BaseTableOwner) o;
        return path.equals(that.path) && baeTable.equals(that.baeTable);
    }

    @Override
    public int hashCode() {
        int result = baeTable.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BaseTableOwner{" +
                "table=" + baeTable +
                ", path=" + path +
                '}';
    }

    public static BaseTableOwner of(TableLike<?> table) {
        if (table instanceof TableProxy<?>) {
            TableProxy<?> proxy = (TableProxy<?>) table;
            return proxy.__baseTableOwner();
        }
        if (table instanceof TableImplementor<?>) {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) table;
            return tableImplementor.getBaseTableOwner();
        }
        return null;
    }

    public static BaseTableOwner of(Expression<?> expression) {
        if (expression instanceof AbstractTypedEmbeddedPropExpression<?>) {
            AbstractTypedEmbeddedPropExpression<?> embeddedPropExpression = (AbstractTypedEmbeddedPropExpression<?>) expression;
            return embeddedPropExpression.__baseTableOwner();
        }
        if (expression instanceof BaseTableExpression<?>) {
            BaseTableExpression<?> baseTableExpression = (BaseTableExpression<?>) expression;
            return baseTableExpression.getBaseTableOwner();
        }
        return null;
    }
}
