package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicate;
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicateMergedResult;
import org.babyfish.jimmer.sql.ast.impl.base.*;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MergedBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.query.MutableStatementImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractIdentityDataManager;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.TableUsedState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AstContext extends AbstractIdentityDataManager<RealTable, TableUsedState> implements RootTableResolver {

    private final JSqlClientImplementor sqlClient;

    private StatementFrame statementFrame;

    private JoinTypeMergeFrame joinTypeMergeFrame;

    private BaseTableRenderFrame baseTableRenderFrame;

    private int modCount;

    private Set<MergedBaseQueryImpl<?>> visitingRecursiveMergedQueries;

    public AstContext(JSqlClientImplementor sqlClient) {
        this.sqlClient = sqlClient;
    }

    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
    }

    @Override
    protected TableUsedState createValue(RealTable key) {
        return TableUsedState.ID_ONLY;
    }

    public void useTableId(RealTable table) {
        getOrCreateValue(table);
    }

    public void useTable(RealTable table) {
        putValue(table, TableUsedState.USED);
    }

    public TableUsedState getTableUsedState(RealTable table) {
        TableUsedState state = getValue(table);
        return state != null ? state : TableUsedState.NONE;
    }

    public void pushStatement(AbstractMutableStatementImpl statement) {
        this.statementFrame = new StatementFrame(statement, this.statementFrame);
    }

    public void popStatement() {
        this.statementFrame = this.statementFrame.parent;
    }

    public void pushRenderedBaseTable(RealTable realBaseTable) {
        this.baseTableRenderFrame = new BaseTableRenderFrame(
                baseTableRenderFrame,
                realBaseTable
        );
    }

    public void popRenderedBaseTable() {
        this.baseTableRenderFrame = baseTableRenderFrame.parent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> TableImplementor<E> resolveRootTable(Table<E> table) {
        if (table instanceof TableImplementor<?>) {
            return (TableImplementor<E>) table;
        }
        TableImplementor<E> tableImplementor = ((TableProxy<E>)table).__unwrap();
        if (tableImplementor != null) {
            return tableImplementor;
        }
        for (StatementFrame frame = this.statementFrame; frame != null; frame = frame.parent) {
            AbstractMutableStatementImpl statement = frame.statement;
            TableLike<?> stmtTable = statement.getTable();
            BaseTableOwner baseTableOwner = BaseTableOwner.of(table);
            if (stmtTable instanceof BaseTableSymbol) {
                TableImplementor<?> resolved = resolve(statement, (BaseTableSymbol) stmtTable, table);
                if (resolved != null) {
                    return (TableImplementor<E>) resolved.baseTableOwner(baseTableOwner);
                }
            } else if (baseTableOwner != null && baseTableOwner.getBaseTable().getParent() != null) {
                TableImplementor<?> resolved = resolve(statement, baseTableOwner.getBaseTable(), table);
                if (resolved != null) {
                    return (TableImplementor<E>) resolved.baseTableOwner(baseTableOwner);
                }
            } else if (AbstractTypedTable.__refEquals(stmtTable, table)) {
                tableImplementor = (TableImplementor<E>) statement.getTableLikeImplementor();
                return tableImplementor.baseTableOwner(baseTableOwner);
            }
        }
        TableProxy<E> tableProxy = (TableProxy<E>) table;
        if (tableProxy.__parent() != null) {
            throw new IllegalArgumentException(
                    "\"" +
                            AstContext.class.getName() +
                            ".resolveRootTable\" only does not accept non-root table, you can use \"" +
                            TableProxies.class.getName() +
                            ".resolve\""
            );
        }
        throw new IllegalArgumentException("Cannot resolve the root table " + table);
    }

    private TableImplementor<?> resolve(AbstractMutableStatementImpl statement, BaseTableSymbol rootBaseTableSymbol, Table<?> table) {
        TableImplementor<?> resolved = rootBaseTableSymbol.getQuery().resolveRootTable(table);
        if (resolved != null) {
            return resolved;
        }
        if (!(table instanceof TableProxy<?>)) {
            return null;
        }
        BaseTableOwner baseTableOwner = ((TableProxy<?>) table).__baseTableOwner();
        if (baseTableOwner == null) {
            return null;
        }
        BaseTableImplementor baseTableImplementor = resolveBaseTable(statement, baseTableOwner.getBaseTable());
        return baseTableImplementor.getQuery().resolveRootTable(table);
    }

    public BaseTableImplementor resolveBaseTable(BaseTableSymbol baseTable) {
        for (StatementFrame frame = statementFrame; frame != null; frame = frame.parent) {
            BaseTableImplementor baseTableImplementor = resolveBaseTable(frame.statement, baseTable);
            if (baseTableImplementor != null) {
                return baseTableImplementor;
            }
        }
        return null;
    }

    private BaseTableImplementor resolveBaseTable(AbstractMutableStatementImpl statement, BaseTableSymbol baseTable) {
        TableLike<?> parent = baseTable.getParent();
        if (parent == null) {
            TableLikeImplementor<?> implementor = statement.getTableLikeImplementor();
            if (implementor instanceof BaseTableImplementor) {
                return (BaseTableImplementor) implementor;
            }
            return null;
        }
        TableLikeImplementor<?> parentImplementor =
                parent instanceof BaseTableSymbol ?
                        resolveBaseTable(statement, (BaseTableSymbol) parent) :
                        TableProxies.resolve((Table<?>) parent, this);
        if (parentImplementor == null) {
            return null;
        }
        BaseTableSymbol recursive = baseTable.getRecursive();
        BaseTableImplementor recursiveImplementor = recursive != null ?
                resolveBaseTable(recursive) :
                null;
        return BaseTableImpl.of(baseTable, parentImplementor, recursiveImplementor);
    }

    public AbstractMutableStatementImpl getStatement() {
        return statementFrame.statement;
    }

    public JoinTypeMergeScope getJoinTypeMergeScope() {
        JoinTypeMergeFrame frame = this.joinTypeMergeFrame;
        return frame != null ? frame.scope : null;
    }

    public void pushJoinTypeMergeScope(JoinTypeMergeScope scope) {
        joinTypeMergeFrame = new JoinTypeMergeFrame(scope, joinTypeMergeFrame);
    }

    public void popJoinTypeMergeScope() {
        joinTypeMergeFrame = joinTypeMergeFrame.parent;
    }

    public void pushVirtualPredicateContext(VirtualPredicate.Op op) {
        statementFrame.pushVpf(op);
    }

    public void popVirtualPredicateContext() {
        statementFrame.popVpf();
    }

    @SuppressWarnings("unchecked")
    public <T> T resolveVirtualPredicate(T expression) {
        if (expression == null) {
            return null;
        }
        Unwrapped<T> unwrapped = Unwrapped.of(expression);
        if (unwrapped.value instanceof VirtualPredicate) {
            T resolved = (T) statementFrame.peekVpf().add((VirtualPredicate) unwrapped.value);
            return unwrapped.wrapAgain(resolved);
        }
        if (expression instanceof Ast && ((Ast)expression).hasVirtualPredicate()) {
            return (T) ((Ast)expression).resolveVirtualPredicate(AstContext.this);
        }
        if (expression instanceof MutableStatementImplementor && ((MutableStatementImplementor)expression).hasVirtualPredicate()) {
            ((MutableStatementImplementor)expression).resolveVirtualPredicate(AstContext.this);
            return expression;
        }
        return expression;
    }

    public <T> List<T> resolveVirtualPredicates(List<T> expressions) {
        boolean changed = false;
        for (T expression : expressions) {
            if (expression instanceof Ast && ((Ast)expression).hasVirtualPredicate()) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return expressions;
        }
        List<T> newExpressions = new ArrayList<>(expressions.size());
        for (T expression : expressions) {
            T newExpression = resolveVirtualPredicate(expression);
            if (newExpression != null) {
                newExpressions.add(newExpression);
            }
        }
        VirtualPredicateMergedResult.removeEmptyResult(newExpressions);
        return newExpressions;
    }

    public Predicate[] resolveVirtualPredicates(Predicate[] predicates) {
        boolean changed = false;
        for (Predicate predicate : predicates) {
            if (((Ast)predicate).hasVirtualPredicate()) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return predicates;
        }
        List<Predicate> newPredicates = new ArrayList<>(predicates.length);
        for (Predicate predicate : predicates) {
            Predicate newPredicate = resolveVirtualPredicate(predicate);
            if (newPredicate != null) {
                newPredicates.add(newPredicate);
            }
        }
        VirtualPredicateMergedResult.removeEmptyResult(newPredicates);
        return newPredicates.toArray(PredicateImplementor.EMPTY_PREDICATES);
    }

    public int modCount() {
        return modCount;
    }

    @Nullable
    public BaseSelectionMapper getBaseSelectionMapper(BaseTableOwner baseTableOwner) {
        if (baseTableOwner == null) {
            return null;
        }
        BaseTableSymbol recursive = baseTableOwner.getBaseTable().getRecursive();
        if (recursive != null) {
            baseTableOwner = new BaseTableOwner(recursive, baseTableOwner.getIndex());
        }
        BaseTableSymbol baseTable = baseTableOwner.getBaseTable();
        boolean cte = baseTable.isCte();
        for (StatementFrame frame = statementFrame; frame != null && frame.usingBaseQuery; frame = frame.parent) {
            if (BaseTableSymbols.contains(frame.statement.getTable(), baseTable)) {
                BaseQueryScope scope = frame.baseQueryScope();
                MergedBaseQueryImpl<?> mergedBy = MergedBaseQueryImpl.from(baseTable.getQuery());
                if (mergedBy != null) {
                    for (TypedBaseQueryImplementor<?> itemQuery : mergedBy.getExpandedQueries()) {
                        scope.mapper(new BaseTableOwner(itemQuery.asBaseTable(null, cte), baseTableOwner.getIndex()));
                    }
                }
                return scope.mapper(baseTableOwner);
            }
        }
        return null;
    }

    @Nullable
    public BaseSelectionAliasRender getBaseSelectionRender(ConfigurableBaseQuery<?> query) {
        for (StatementFrame frame = statementFrame; frame != null && frame.usingBaseQuery; frame = frame.parent) {
            if (TableUtils.hasBaseTable(frame.statement.getTableLikeImplementor())) {
                return frame.baseQueryScope().toBaseSelectionRender(query);
            }
        }
        return null;
    }

    public RealTable getRenderedRealBaseTable() {
        BaseTableRenderFrame frame = baseTableRenderFrame;
        if (frame != null && frame.realTable != null) {
            return frame.realTable;
        }
        throw new IllegalStateException("No rendered real base table");
    }

    public void visitRecursiveQuery(MergedBaseQueryImpl<?> query, Runnable runnable) {
        if (!query.isRecursive()) {
            runnable.run();
            return;
        }
        Set<MergedBaseQueryImpl<?>> set = visitingRecursiveMergedQueries;
        if (set == null) {
            visitingRecursiveMergedQueries = set = new HashSet<>();
        }
        if (set.add(query)) {
            try {
                runnable.run();
            } finally {
                set.remove(query);
            }
        }
    }

    private static class Unwrapped<T> {

        final T value;

        private final PredicateWrapper wrapper;

        private Unwrapped(T value, PredicateWrapper wrapper) {
            this.value = value;
            this.wrapper = wrapper;
        }

        @SuppressWarnings("unchecked")
        static <T> Unwrapped<T> of(T value) {
            if (!(value instanceof PredicateWrapper)) {
                return new Unwrapped<>(value, null);
            }
            PredicateWrapper wrapper = (PredicateWrapper) value;
            return new Unwrapped<>((T)wrapper.unwrap(), wrapper);
        }

        @SuppressWarnings("unchecked")
        T wrapAgain(T value) {
            PredicateWrapper wrapper = this.wrapper;
            if (wrapper != null && value != null) {
                return (T) wrapper.wrap(value);
            }
            return value;
        }
    }

    private class StatementFrame {

        final AbstractMutableStatementImpl statement;

        final StatementFrame parent;

        final boolean usingBaseQuery;

        private VirtualPredicateFrame vpFrame;

        private BaseQueryScope baseQueryScope;

        private boolean baseQueryResolved;

        private StatementFrame(AbstractMutableStatementImpl statement, StatementFrame parent) {
            this.statement = statement;
            this.parent = parent;
            boolean usingBaseQuery;
            if (parent != null && parent.usingBaseQuery) {
                usingBaseQuery = true;
            } else {
                TableLikeImplementor<?> tableLikeImplementor = statement.getTableLikeImplementor();
                usingBaseQuery = TableUtils.hasBaseTable(tableLikeImplementor);
            }
            this.usingBaseQuery = usingBaseQuery;
        }

        public VirtualPredicateFrame peekVpf() {
            VirtualPredicateFrame vpFrame = this.vpFrame;
            if (vpFrame == null) {
                this.vpFrame = vpFrame = new VirtualPredicateFrame(VirtualPredicate.Op.AND, null);
            }
            return vpFrame;
        }

        public void pushVpf(VirtualPredicate.Op op) {
            this.vpFrame = new VirtualPredicateFrame(op, this.peekVpf());
        }

        public void popVpf() {
            this.vpFrame = vpFrame.parent;
        }

        public BaseQueryScope baseQueryScope() {
            if (!baseQueryResolved) {
                baseQueryScope = createBaseQueryScope();
                baseQueryResolved = true;
            }
            return baseQueryScope;
        }

        private BaseQueryScope createBaseQueryScope() {
            if (!usingBaseQuery) {
                return null;
            }
            return new BaseQueryScope(AstContext.this);
        }
    }

    private class VirtualPredicateFrame extends AbstractDataManager<VirtualPredicate, VirtualPredicateMergedResult> {

        private final VirtualPredicate.Op op;

        private final VirtualPredicateFrame parent;

        private VirtualPredicateFrame(VirtualPredicate.Op op, VirtualPredicateFrame parent) {
            this.op = op;
            this.parent = parent;
        }

        @Override
        protected int hashCode(VirtualPredicate key) {
            return System.identityHashCode(key.getTableImplementor(AstContext.this)) ^ key.getSubKey().hashCode();
        }

        @Override
        protected boolean equals(VirtualPredicate key1, VirtualPredicate key2) {
            return key1.getSubKey().equals(key2.getSubKey()) &&
                    key1.getTableImplementor(AstContext.this) == key2.getTableImplementor(AstContext.this);
        }

        public Predicate add(VirtualPredicate virtualPredicate) {
            VirtualPredicateMergedResult result = getValue(virtualPredicate);
            if (result != null) {
                result.merge(virtualPredicate);
                return null;
            }
            result = new VirtualPredicateMergedResult(getStatement(), op);
            putValue(virtualPredicate, result);
            result.merge(virtualPredicate);
            modCount++;
            return result;
        }
    }

    private static class JoinTypeMergeFrame {

        final JoinTypeMergeScope scope;

        final JoinTypeMergeFrame parent;

        JoinTypeMergeFrame(JoinTypeMergeScope scope, JoinTypeMergeFrame parent) {
            this.scope = scope;
            this.parent = parent;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        boolean addSp = false;
        for (StatementFrame frame = this.statementFrame; frame != null; frame = frame.parent) {
            if (addSp) {
                builder.append("->");
            } else {
                addSp = true;
            }
            builder.append(frame.statement);
        }
        return builder.toString();
    }

    private static class BaseTableRenderFrame {

        final BaseTableRenderFrame parent;

        final RealTable realTable;

        BaseTableRenderFrame(BaseTableRenderFrame parent, RealTable realTable) {
            this.parent = parent;
            this.realTable = realTable;
        }

        @Override
        public String toString() {
            return "BaseTableRenderFrame{" +
                    "parent=" + parent +
                    ", realTable=" + realTable +
                    '}';
        }
    }
}
