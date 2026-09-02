package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.query.selectable.ReturningSelectable;
import org.babyfish.jimmer.sql.ast.table.BaseTable;

/**
 * Bulk insert whose rows are supplied by a typed {@link BaseTable} source.
 */
public interface MutableInsert<S extends BaseTable>
        extends Executable<Integer>, ReturningSelectable {

    @OldChain
    <T> MutableInsert<S> set(PropExpression<T> target, Expression<T> source);

    /**
     * Skip conflicts for the highest-priority eligible id/key group inferred
     * from target metadata.
     */
    @OldChain
    MutableInsert<S> onConflictDoNothing();

    /**
     * Skip conflicts for one explicitly specified unique physical key.
     * The array must not be empty; call {@link #onConflictDoNothing()} to use
     * metadata inference.
     */
    @OldChain
    MutableInsert<S> onConflictDoNothing(PropExpression<?>... targetProps);
}
