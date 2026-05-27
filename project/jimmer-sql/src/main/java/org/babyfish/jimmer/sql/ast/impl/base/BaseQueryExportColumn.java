package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.List;
import java.util.Objects;

public final class BaseQueryExportColumn {

    private final List<RealTable.Key> tableKeys;

    private final String name;

    private final FormulaTemplate formula;

    private final boolean foreignKeyInBaseQuery;

    private final int index;

    BaseQueryExportColumn(
            List<RealTable.Key> tableKeys,
            String name,
            boolean foreignKeyInBaseQuery,
            int index
    ) {
        this.tableKeys = tableKeys;
        this.name = name;
        this.formula = null;
        this.foreignKeyInBaseQuery = foreignKeyInBaseQuery;
        this.index = index;
    }

    BaseQueryExportColumn(
            List<RealTable.Key> tableKeys,
            FormulaTemplate formula,
            int index
    ) {
        this.tableKeys = tableKeys;
        this.name = null;
        this.formula = formula;
        this.foreignKeyInBaseQuery = false;
        this.index = index;
    }

    public List<RealTable.Key> getTableKeys() {
        return tableKeys;
    }

    public String getName() {
        return name;
    }

    public FormulaTemplate getFormula() {
        return formula;
    }

    public boolean isForeignKeyInBaseQuery() {
        return foreignKeyInBaseQuery;
    }

    public int getIndex() {
        return index;
    }

    Key key() {
        return new Key(tableKeys, name, formula, foreignKeyInBaseQuery);
    }

    static final class Key {

        final List<RealTable.Key> tableKeys;

        final String name;

        final FormulaTemplate formula;

        final boolean foreignKeyInBaseQuery;

        Key(
                List<RealTable.Key> tableKeys,
                String name,
                FormulaTemplate formula,
                boolean foreignKeyInBaseQuery
        ) {
            this.tableKeys = tableKeys;
            this.name = name;
            this.formula = formula;
            this.foreignKeyInBaseQuery = foreignKeyInBaseQuery;
        }

        @Override
        public int hashCode() {
            int result = tableKeys.hashCode();
            result = 31 * result + Objects.hashCode(name);
            result = 31 * result + Objects.hashCode(formula);
            result = 31 * result + Boolean.hashCode(foreignKeyInBaseQuery);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key that = (Key) o;
            return tableKeys.equals(that.tableKeys) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(formula, that.formula) &&
                    foreignKeyInBaseQuery == that.foreignKeyInBaseQuery;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "tableKeys=" + tableKeys +
                    ", name='" + name + '\'' +
                    ", formula=" + formula +
                    ", foreignKeyInBaseQuery=" + foreignKeyInBaseQuery +
                    '}';
        }
    }
}
