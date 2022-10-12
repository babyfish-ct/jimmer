package org.babyfish.jimmer.sql.fluent.impl;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableDeleteImpl;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.fluent.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class FluentImpl implements Fluent {

    private final JSqlClient sqlClient;

    private final List<Filterable> stack = new LinkedList<>();

    public FluentImpl(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public <T extends FluentTable<?>> FluentRootQuery<T> query(T table) {
        Objects.requireNonNull(table);
        if (table instanceof TableEx<?>) {
            throw new IllegalArgumentException("Top-level query does not accept TableEx");
        }
        if (!stack.isEmpty()) {
            throw new IllegalArgumentException("Top-level query cannot be created inside another statement");
        }
        ImmutableType immutableType = ImmutableType.get(table.getClass());
        MutableRootQueryImpl<T> query = new MutableRootQueryImpl<>(sqlClient, immutableType, false);
        table.bind(query.getTable());
        stack.add(query);
        return new FluentRootQueryImpl<>(query, () -> pop(query));
    }

    @Override
    public FluentSubQuery subQuery(FluentTable<?> table) {
        Objects.requireNonNull(table);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("No parent can be found in fluent");
        }
        Filterable parent = stack.get(stack.size() - 1);
        ImmutableType immutableType = ImmutableType.get(table.getClass());
        MutableSubQueryImpl subQuery = new MutableSubQueryImpl((AbstractMutableStatementImpl) parent, immutableType);
        table.bind(subQuery.getTable());
        stack.add(subQuery);
        return new FluentSubQueryImpl(subQuery, () -> pop(subQuery));
    }

    @Override
    public FluentUpdate update(FluentTable<?> table) {
        Objects.requireNonNull(table);
        if (!stack.isEmpty()) {
            throw new IllegalArgumentException("Update statement cannot be created inside another statement");
        }
        ImmutableType immutableType = ImmutableType.get(table.getClass());
        MutableUpdateImpl update = new MutableUpdateImpl(sqlClient, immutableType);
        table.bind(update.getTable());
        stack.add(update);
        return new FluentUpdateImpl(update, () -> pop(update));
    }

    @Override
    public FluentDelete delete(FluentTable<?> table) {
        Objects.requireNonNull(table);
        if (!stack.isEmpty()) {
            throw new IllegalArgumentException("Delete statement cannot be created inside another statement");
        }
        ImmutableType immutableType = ImmutableType.get(table.getClass());
        MutableDeleteImpl delete = new MutableDeleteImpl(sqlClient, immutableType);
        table.bind(delete.getTable());
        stack.add(delete);
        return new FluentDeleteImpl(delete, () -> pop(delete));
    }

    private void pop(AbstractMutableStatementImpl statement) {
        if (stack.get(stack.size() - 1) != statement) {
            throw new IllegalStateException("Nested sub query is not frozen");
        }
        stack.remove(stack.size() - 1);
    }
}
