package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicate;
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicateMergedResult;
import org.babyfish.jimmer.sql.ast.impl.query.MutableStatementImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractDataManager;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractIdentityDataManager;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.TableUsedState;

import java.util.ArrayList;
import java.util.List;

public class AstContext extends AbstractIdentityDataManager<TableImplementor<?>, TableUsedState> implements RootTableResolver {

    private final JSqlClientImplementor sqlClient;

    private StatementFrame statementFrame;

    private int modCount;

    public AstContext(JSqlClientImplementor sqlClient) {
        this.sqlClient = sqlClient;
    }

    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
    }

    @Override
    protected TableUsedState createValue(TableImplementor<?> key) {
        return TableUsedState.ID_ONLY;
    }

    public void useTableId(TableImplementor<?> tableImplementor) {
        getOrCreateValue(tableImplementor);
    }

    public void useTable(TableImplementor<?> tableImplementor) {
        putValue(tableImplementor, TableUsedState.USED);
    }

    public TableUsedState getTableUsedState(TableImplementor<?> tableImplementor) {
        TableUsedState state = getValue(tableImplementor);
        return state != null ? state : TableUsedState.NONE;
    }

    public void pushStatement(AbstractMutableStatementImpl statement) {
        StatementFrame frame = this.statementFrame;
        this.statementFrame = new StatementFrame(statement, frame);
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
            Table<?> stmtTable = statement.getTable();
            if (AbstractTypedTable.__refEquals(stmtTable, table)) {
                return (TableImplementor<E>) statement.getTableImplementor();
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
        T unwrapped = unwrap(expression);
        if (unwrapped instanceof VirtualPredicate) {
            return (T) statementFrame.peekVpf().add((VirtualPredicate) unwrapped);
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
        VirtualPredicateFrame vpf = statementFrame.peekVpf();
        List<T> newExpressions = new ArrayList<>(expressions.size());
        for (T expression : expressions) {
            T newExpression = resolveVirtualPredicate(expression);
            if (newExpression != null) {
                newExpressions.add(newExpression);
            }
        }
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
        VirtualPredicateFrame vpf = statementFrame.peekVpf();
        List<Predicate> newPredicates = new ArrayList<>(predicates.length);
        for (Predicate predicate : predicates) {
            Predicate newPredicate = resolveVirtualPredicate(predicate);
            if (newPredicate != null) {
                newPredicates.add(newPredicate);
            }
        }
        return newPredicates.toArray(PredicateImplementor.EMPTY_PREDICATES);
    }

    public int modCount() {
        return modCount;
    }

    @SuppressWarnings("unchecked")
    private static <T> T unwrap(T o) {
        while (true) {
            if (!(o instanceof PredicateWrapper)) {
                return o;
            }
            o = (T)((PredicateWrapper)o).unwrap();
        }
    }

    private class StatementFrame {

        final AbstractMutableStatementImpl statement;

        final StatementFrame parent;

        private VirtualPredicateFrame vpFrame;

        private StatementFrame(AbstractMutableStatementImpl statement, StatementFrame parent) {
            this.statement = statement;
            this.parent = parent;
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
}
