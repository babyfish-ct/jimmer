package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.util.AbstractIdentityDataManager;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.TableUsedState;

public class AstContext extends AbstractIdentityDataManager<TableImplementor<?>, TableUsedState> implements RootTableResolver {

    private final JSqlClientImplementor sqlClient;

    private StackFrame frame;

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
        StackFrame frame = this.frame;
        if (frame != null && frame.statement.isSubQueryDisabled()) {
            throw new IllegalStateException(
                    "Cannot use sub query here because the sub query of parent statement is disabled"
            );
        }
        this.frame = new StackFrame(statement, frame);
    }

    public void popStatement() {
        this.frame = this.frame.parent;
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
        for (StackFrame frame = this.frame; frame != null; frame = frame.parent) {
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
        return frame.statement;
    }

    private static class StackFrame {

        final AbstractMutableStatementImpl statement;

        final StackFrame parent;

        private StackFrame(AbstractMutableStatementImpl statement, StackFrame parent) {
            this.statement = statement;
            this.parent = parent;
        }
    }
}
