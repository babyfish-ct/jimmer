package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableSelectionLayout;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableShape;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class AbstractBaseTableSymbol implements BaseTableSymbol {

    private final TypedBaseQueryImplementor<?> query;

    protected final List<Selection<?>> selections;

    protected final BaseTableSelectionLayout selectionLayout;

    protected final BaseTableShape<?, ?> shape;

    protected final BaseTableKind kind;

    protected final BaseTableSymbol recursive;

    protected final TableLike<?> parent;

    private final WeakJoinHandle handle;

    private final JoinType joinType;

    protected AbstractBaseTableSymbol(
            TypedBaseQueryImplementor<?> query,
            List<Selection<?>> selections,
            BaseTableSelectionLayout selectionLayout,
            BaseTableKind kind,
            BaseTableShape<?, ?> shape
    ) {
        this.query = query;
        this.selections = wrapSelections(selections);
        this.selectionLayout = selectionLayout;
        this.shape = shape;
        this.kind = kind;
        this.parent = null;
        this.handle = null;
        this.joinType = JoinType.INNER;
        this.recursive = null;
    }

    protected AbstractBaseTableSymbol(
            BaseTableSymbol base,
            TableLike<?> parent,
            WeakJoinHandle handle,
            JoinType joinType,
            BaseTableSymbol recursive
    ) {
        this.query = base.getQuery();
        this.selections = wrapSelections(base.getSelections());
        this.selectionLayout = base.getSelectionLayout();
        this.shape = base.getShape();
        this.kind = base.isRecursiveCte() ?
                BaseTableKind.RECURSIVE_CTE :
                base.isCte() ? BaseTableKind.CTE : BaseTableKind.DERIVED;
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.handle = Objects.requireNonNull(handle, "handle cannot be null");
        this.joinType = joinType;
        this.recursive = recursive;
    }

    private List<Selection<?>> wrapSelections(List<Selection<?>> selections) {
        return wrapSelections(selections, this);
    }

    public BaseTableSelectionLayout getSelectionLayout() {
        return selectionLayout;
    }

    @Override
    public BaseTableShape<?, ?> getShape() {
        return shape;
    }

    public static List<Selection<?>> wrapSelections(List<Selection<?>> selections, BaseTable baseTable) {
        int size = selections.size();
        List<Selection<?>> wrappedSelections = new ArrayList<>(selections.size());
        for (int i = 0; i < size; i++) {
            Selection<?> wrappedSelection = BaseTableSelections.of(
                    selections.get(i),
                    baseTable,
                    i
            );
            wrappedSelections.add(wrappedSelection);
        }
        return Collections.unmodifiableList(wrappedSelections);
    }

    @Override
    public ConfigurableBaseQueryImpl<?> getQuery() {
        return query.firstConfigurableQuery();
    }

    @Override
    public List<Selection<?>> getSelections() {
        return selections;
    }

    @Override
    public TableLike<?> getParent() {
        return parent;
    }

    @Override
    public WeakJoinHandle getWeakJoinHandle() {
        return handle;
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public boolean isCte() {
        return kind != BaseTableKind.DERIVED;
    }

    @Override
    public boolean isRecursiveCte() {
        return kind == BaseTableKind.RECURSIVE_CTE;
    }

    @Override
    public BaseTableSymbol getRecursive() {
        return recursive;
    }

    public abstract AbstractBaseTableSymbol query(TypedBaseQueryImplementor<?> query);

    protected final String suffix() {
        return kind == BaseTableKind.RECURSIVE_CTE ?
                "(RecursiveCTE)" :
                kind == BaseTableKind.CTE ? "(CTE)" : "";
    }

    public static <T extends BaseTable> T validateCte(T baseTable, boolean cte) {
        BaseTableSymbol symbol = (BaseTableSymbol) BaseTableProxies.unwrap(baseTable);
        if (symbol.isCte() != cte) {
            throw new IllegalStateException(
                    "BaseQuery does not support calling " +
                            "`asBaseTable`/`asCteBaseTable` " +
                            "multiple times with different parameters."
            );
        }
        return baseTable;
    }
}
