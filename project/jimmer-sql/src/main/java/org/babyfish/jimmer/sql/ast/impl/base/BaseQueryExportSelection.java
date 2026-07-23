package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BaseQueryExportSelection {

    protected final BaseQueryExport export;

    private final int index;

    private final RealTable rootRealTable;

    private final int rootDepth;

    BaseQueryExportSelection(BaseQueryExport export, int index, RealTable rootRealTable) {
        this.export = export;
        this.index = index;
        this.rootRealTable = rootRealTable;
        this.rootDepth = depth(rootRealTable);
    }

    int getIndex() {
        return index;
    }

    RealTable getRealBaseTable() {
        return export.getRealBaseTable();
    }

    public boolean isTableBacked() {
        return rootRealTable != null;
    }

    public RealTable getRootRealTable() {
        return rootRealTable;
    }

    public boolean isRootTable(RealTable table) {
        return rootRealTable != null &&
                (table == rootRealTable ||
                        depth(table) == rootDepth && samePath(table, rootRealTable));
    }

    public boolean containsTable(RealTable table) {
        return rootRealTable != null && relativeDepth(table) != -1;
    }

    public int columnIndex(RealTable table, String columnName, boolean foreignKeyInBaseQuery) {
        return export
                .column(this, tableKeys(table), columnName, foreignKeyInBaseQuery)
                .getIndex();
    }

    public Integer columnIndexOrNull(RealTable table, String columnName, boolean foreignKeyInBaseQuery) {
        return export.columnIndexOrNull(this, tableKeys(table), columnName, foreignKeyInBaseQuery);
    }

    @Nullable
    Integer columnIndexIfContained(RealTable table, String columnName, boolean foreignKeyInBaseQuery) {
        List<RealTable.Key> tableKeys = tableKeysOrNull(table);
        return tableKeys != null ?
                export.column(this, tableKeys, columnName, foreignKeyInBaseQuery).getIndex() :
                null;
    }

    public int formulaIndex(RealTable table, FormulaTemplate formula) {
        return export
                .formula(this, tableKeys(table), formula)
                .getIndex();
    }

    @Nullable
    Integer formulaIndexIfContained(RealTable table, FormulaTemplate formula) {
        List<RealTable.Key> tableKeys = tableKeysOrNull(table);
        return tableKeys != null ?
                export.formula(this, tableKeys, formula).getIndex() :
                null;
    }

    public int expressionIndex() {
        return export.expressionIndex(this);
    }

    public Collection<BaseQueryExportColumn> columns() {
        return export.columns(index);
    }

    protected List<RealTable.Key> tableKeys(RealTable table) {
        if (rootRealTable == null) {
            throw new IllegalStateException("Current base-query selection is not table-backed");
        }
        List<RealTable.Key> tableKeys = tableKeysOrNull(table);
        if (tableKeys == null) {
            throw new IllegalArgumentException("The table is not inside the current base-query selection");
        }
        return tableKeys;
    }

    @Nullable
    private List<RealTable.Key> tableKeysOrNull(RealTable table) {
        int relativeDepth = relativeDepth(table);
        if (relativeDepth == -1) {
            return null;
        }
        if (relativeDepth == 0) {
            // Foreign-key id-view exports can be selected from a joined table while
            // their physical columns are stored on that table's parent. They still
            // belong to the selected row export, whose structured path is empty.
            return Collections.emptyList();
        }
        RealTable.Key[] tableKeys = new RealTable.Key[relativeDepth];
        RealTable current = table;
        for (int i = relativeDepth - 1; i >= 0; i--) {
            tableKeys[i] = current.getKey();
            current = current.getParent();
        }
        return Arrays.asList(tableKeys);
    }

    private int relativeDepth(RealTable table) {
        if (rootRealTable == null) {
            return -1;
        }
        if (table == rootRealTable) {
            return 0;
        }
        int tableDepth = depth(table);
        if (tableDepth <= rootDepth) {
            RealTable rootAncestor = rootRealTable;
            for (int i = tableDepth; i < rootDepth; i++) {
                rootAncestor = rootAncestor.getParent();
            }
            return samePath(table, rootAncestor) ? 0 : -1;
        }
        int relativeDepth = tableDepth - rootDepth;
        RealTable current = table;
        for (int i = 0; i < relativeDepth; i++) {
            if (current.getTableLikeImplementor().getWeakJoinHandle() != null) {
                return -1;
            }
            current = current.getParent();
        }
        return samePath(current, rootRealTable) ? relativeDepth : -1;
    }

    private static boolean samePath(RealTable a, RealTable b) {
        while (a != b) {
            if (a == null || b == null || !Objects.equals(a.getKey(), b.getKey())) {
                return false;
            }
            a = a.getParent();
            b = b.getParent();
        }
        return true;
    }

    private static int depth(RealTable table) {
        int depth = 0;
        for (RealTable current = table; current != null; current = current.getParent()) {
            depth++;
        }
        return depth;
    }
}
