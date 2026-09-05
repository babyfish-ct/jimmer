package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.query.selectable.ReturningSelectable;
import org.babyfish.jimmer.sql.ast.table.BaseTable;

/**
 * Bulk insert whose rows are supplied by a typed {@link BaseTable} source.
 * Conflicts fail by default; call {@link #onConflictDoNothing()} or its explicit-key
 * overload to skip conflicts for a selected key.
 *
 * <p>Returning selections contain physical target values of newly inserted rows,
 * including generated and default values. Skipped conflicts are excluded and
 * return order is not guaranteed. The affected-row count returned by {@code execute}
 * follows the database and JDBC driver conventions and is not normalized across dialects.
 */
public interface MutableInsert<S extends BaseTable>
        extends Executable<Integer>, ReturningSelectable {

    /**
     * Assign an insert value from the source or a constant expression.
     * Each physical target column can be assigned only once. Omitted columns
     * retain their database defaults or normal framework initialization.
     *
     * @param target a physical property of the mutation target
     * @param source the value to insert; it cannot reference the existing target row
     * @param <T> the value type
     * @return this command
     */
    @OldChain
    <T> MutableInsert<S> set(PropExpression<T> target, Expression<T> source);

    /**
     * Skip conflicts for the highest-priority eligible id/key group inferred
     * from target metadata.
     * An eligible group must have insert assignments for all of its columns.
     * The id is preferred, followed by key groups in metadata order. For an
     * association table, the source-id/target-id pair is used.
     *
     * <p>This selects one conflict key, not every unique constraint of the table.
     * If no eligible group exists, execution fails before mutation. A later call
     * to either overload replaces the previous conflict-key configuration.
     *
     * @return this command
     */
    @OldChain
    MutableInsert<S> onConflictDoNothing();

    /**
     * Skip conflicts for one explicitly specified unique physical key.
     * The array must not be empty; call {@link #onConflictDoNothing()} to use
     * metadata inference.
     * The properties must belong to the mutation target, cover one complete id
     * or metadata-declared key group, and have insert assignments for every column.
     * For association tables, specify both source id and target id.
     *
     * <p>Only conflicts for the selected key are skipped. A later call to either
     * overload replaces the previous conflict-key configuration.
     *
     * @param targetProps the non-empty, duplicate-free physical conflict key
     * @return this command
     * @throws IllegalArgumentException if the array is null or empty, or a property
     * does not belong to the target or is not backed by physical columns
     */
    @OldChain
    MutableInsert<S> onConflictDoNothing(PropExpression<?>... targetProps);
}
