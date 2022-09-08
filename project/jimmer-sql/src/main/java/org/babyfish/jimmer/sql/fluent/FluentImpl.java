package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class FluentImpl implements Fluent {

    private final JSqlClient sqlClient;

    private final List<Filterable> stack = new LinkedList<>();

    public FluentImpl(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public <T extends AbstractTableWrapper<?>> MutableRootQuery<T> query(T table) {
        Objects.requireNonNull(table);
        if (table instanceof TableEx<?>) {
            throw new IllegalArgumentException("Top-level query does not accept TableEx");
        }
        if (!stack.isEmpty()) {
            throw new IllegalArgumentException("Top-level query cannot be created inside another query");
        }
        ImmutableType immutableType = ImmutableType.get(table.getClass());
        MutableRootQueryImpl<T> query = new MutableRootQueryImpl<>(
                sqlClient,
                immutableType,
                false
        );
        table.bind(query.getTable());
        stack.add(query);
        return new FluentRootQueryProxy<>(query, () -> {
            if (query.freeze()) {
                if (stack.get(stack.size() - 1) != query) {
                    throw new IllegalStateException("Nested sub query is not frozen");
                }
                stack.remove(stack.size() - 1);
            }
        });
    }

    @Override
    public MutableSubQuery subQuery(AbstractTableWrapper<?> table) {
        Objects.requireNonNull(table);
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("No parent can be found in fluent");
        }
        Filterable parent = stack.get(stack.size() - 1);
        ImmutableType immutableType = ImmutableType.get(table.getClass());
        MutableSubQueryImpl subQuery = new MutableSubQueryImpl(
                (AbstractMutableStatementImpl) parent,
                immutableType,
                false
        );
        table.bind(subQuery.getTable());
        stack.add(subQuery);
        return new FluentSubQueryProxy(subQuery, () -> {
            if (subQuery.freeze()) {
                if (stack.get(stack.size() - 1) != subQuery) {
                    throw new IllegalStateException("Nested sub query is not frozen");
                }
                stack.remove(stack.size() - 1);
            }
        });
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>> MutableRootQuery<AssociationTable<SE, ST, TE, TT>> query(Class<ST> sourceTableType, Function<ST, TT> targetTableGetter) {
        return null;
    }

    @Override
    public <SE, ST extends Table<SE>, TE, TT extends Table<TE>> MutableSubQuery subQuery(Class<ST> sourceTableType, Function<ST, TT> targetTableGetter) {
        return null;
    }
}
