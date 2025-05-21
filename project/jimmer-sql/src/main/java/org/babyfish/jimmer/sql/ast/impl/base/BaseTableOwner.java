package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.BaseTableImplementor;

public class BaseTableOwner {

    final BaseTableImplementor<?> table;

    final int index;

    public BaseTableOwner(BaseTableImplementor<?> table, int index) {
        this.table = table;
        this.index = index;
    }

    public BaseTableImplementor<?> getTable() {
        return table;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BaseTableOwner that = (BaseTableOwner) o;
        return index == that.index && table.equals(that.table);
    }

    @Override
    public int hashCode() {
        int result = table.hashCode();
        result = 31 * result + index;
        return result;
    }

    @Override
    public String toString() {
        return "BaseTableOwner{" +
                "table=" + table +
                ", index=" + index +
                '}';
    }
}
