package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTableWrapper;

import java.util.function.Function;

/**
 * A query created by a Fluent object cannot be used to create sub queries,
 * but needs to call the subQuery of the Fluent object.
 *
 * Fluent has a short life cycle, it needs to be created for each query.
 */
public interface Fluent {

    <T extends AbstractTableWrapper<?>> MutableRootQuery<T> query(T table);

    MutableSubQuery subQuery(AbstractTableWrapper<?> table);

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>> MutableRootQuery<AssociationTable<SE, ST, TE, TT>> query(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter
    );

    <SE, ST extends Table<SE>, TE, TT extends Table<TE>> MutableSubQuery subQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter
    );
}
