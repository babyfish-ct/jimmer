package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicate;
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicateMergedResult;
import org.babyfish.jimmer.sql.ast.impl.base.*;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableBaseQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TypedBaseQueryImplementor;
import org.babyfish.jimmer.sql.ast.impl.query.MutableStatementImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractIdentityDataManager;
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

    private int modCount;

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
            if (stmtTable instanceof BaseTable) {
                TypedBaseQueryImplementor<?> baseQuery = ((BaseTableImplementor)stmtTable).getQuery();
                TableImplementor<?> resolved = baseQuery.resolveRootTable(table);
                if (resolved != null) {
                    resolved.setBaseTableOwner(BaseTableOwner.of(table));
                    return (TableImplementor<E>) resolved;
                }
            } else if (AbstractTypedTable.__refEquals(stmtTable, table)) {
                tableImplementor = (TableImplementor<E>) statement.getTableLikeImplementor();
                tableImplementor.setBaseTableOwner(BaseTableOwner.of(table));
                return tableImplementor;
            }
        }
        if (((TableProxy<E>) table).__parent() != null) {
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

    @SuppressWarnings("unchecked")
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
        BaseTableImplementor baseTable = baseTableOwner.getBaseTable();
        for (StatementFrame frame = statementFrame; frame != null && frame.underBaseQuery; frame = frame.parent) {
            if (MergedBaseTableImplementor.contains(frame.statement.getTable(), baseTable)) {
                return frame.baseQueryScope().mapper(baseTableOwner, baseTable);
            }
        }
        return null;
    }

    @Nullable
    public BaseSelectionRender getBaseSelectionRender(BaseTable baseTable) {
        for (StatementFrame frame = statementFrame; frame != null && frame.underBaseQuery; frame = frame.parent) {
            if (frame.statement.getTable() instanceof BaseTable) {
                return frame.baseQueryScope().toBaseSelectionRender(baseTable);
            }
        }
        return null;
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

        final boolean underBaseQuery;

        private VirtualPredicateFrame vpFrame;

        private BaseQueryScope baseQueryScope;

        private boolean baseQueryResolved;

        private StatementFrame(AbstractMutableStatementImpl statement, StatementFrame parent) {
            this.statement = statement;
            this.parent = parent;
            boolean underBaseQuery;
            if (parent != null && parent.underBaseQuery) {
                underBaseQuery = true;
            } else {
                underBaseQuery = statement.getTable() instanceof BaseTable;
            }
            this.underBaseQuery = underBaseQuery;
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
            if (!underBaseQuery) {
                return null;
            }
            TableLike<?> table = statement.getTable();
            if (table instanceof BaseTable) {
                return new BaseQueryScope((BaseTableImplementor) table);
            }
            return null;
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
}
