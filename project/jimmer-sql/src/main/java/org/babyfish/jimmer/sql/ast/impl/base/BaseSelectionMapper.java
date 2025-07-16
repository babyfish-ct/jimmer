package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.*;

public class BaseSelectionMapper {

    private final BaseQueryScope scope;

    private final RealTable realBaseTable;

    private final int selectionIndex;

    final Map<QualifiedColumn, Integer> columnIndexMap = new LinkedHashMap<>();

    int expressionIndex;

    public BaseSelectionMapper(BaseQueryScope scope, RealTable realBaseTable, int selectionIndex) {
        this.scope = scope;
        this.realBaseTable = realBaseTable;
        this.selectionIndex = selectionIndex;
    }

    public String getAlias() {
        return realBaseTable.getAlias();
    }

    public int columnIndex(String alias, String columnName) {
        AstContext ctx = scope.astContext;
        Selection<?> selection = ((BaseTableImplementor) realBaseTable.getTableLikeImplementor()).getSelections().get(selectionIndex);
        RealTable realTable = TableProxies.resolve((Table<?>) selection, ctx).realTable(ctx);
        List<RealTable.Key> keys = keys(realTable, alias);
        return columnIndexMap.computeIfAbsent(
                new QualifiedColumn(keys, columnName),
                it -> scope.colNo()
        );
    }

    public int formulaIndex(String alias, FormulaTemplate formula) {
        AstContext ctx = scope.astContext;
        Selection<?> selection = ((BaseTableImplementor) realBaseTable.getTableLikeImplementor()).getSelections().get(selectionIndex);
        RealTable realTable = TableProxies.resolve((Table<?>) selection, ctx).realTable(ctx);
        List<RealTable.Key> keys = keys(realTable, alias);
        return columnIndexMap.computeIfAbsent(
                new QualifiedColumn(keys, formula),
                it -> scope.colNo()
        );
    }

    public int expressionIndex() {
        if (expressionIndex == 0) {
            expressionIndex = scope.colNo();
        }
        return expressionIndex;
    }

    private List<RealTable.Key> keys(RealTable table, String alias) {
        List<RealTable.Key> keys = new ArrayList<>();
        keys0(table, alias, keys);
        return keys;
    }

    private void keys0(RealTable table, String alias, List<RealTable.Key> keys) {
        if (table.getAlias().equals(alias)) {
            return;
        }
        RealTable realTable =
                (table.getTableLikeImplementor())
                        .realTable(scope.astContext.getJoinTypeMergeScope());
        for (RealTable childTable : realTable) {
            keys.add(childTable.getKey());
            keys0(childTable, alias, keys);
        }
    }

    static class QualifiedColumn {

        final List<RealTable.Key> keys;

        final String name;

        final FormulaTemplate formula;

        QualifiedColumn(List<RealTable.Key> keys, String name) {
            this.keys = keys;
            this.name = name;
            this.formula = null;
        }

        QualifiedColumn(List<RealTable.Key> keys, FormulaTemplate formula) {
            this.keys = keys;
            this.name = null;
            this.formula = formula;
        }

        @Override
        public int hashCode() {
            int result = keys.hashCode();
            result = 31 * result + Objects.hashCode(name);
            result = 31 * result + Objects.hashCode(formula);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            QualifiedColumn that = (QualifiedColumn) o;
            return keys.equals(that.keys) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(formula, that.formula);
        }

        @Override
        public String toString() {
            return "QualifiedColumn{" +
                    "keys=" + keys +
                    ", name='" + name + '\'' +
                    ", formula=" + formula +
                    '}';
        }
    }
}
