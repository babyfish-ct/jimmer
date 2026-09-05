package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.query.selectable.ReturningSelectable;
import org.babyfish.jimmer.sql.ast.table.BaseTable;

/**
 * Bulk upsert whose rows are supplied by a typed {@link BaseTable} source.
 * Source rows must be unique by the properties declared with {@link #key}; a
 * materialized plan validates this precondition before executing mutations.
 *
 * <p>Each physical target column can be assigned only once across all assignment
 * methods. Insert expressions can read the source; update expressions can read
 * both the source and the existing target row. Omitted insert columns retain
 * their database defaults or normal framework initialization.
 *
 * <p>Returning selections contain physical target values after insertion or an
 * accepted update, including an accepted self-assignment when no update assignment
 * is configured. Rejected updates are excluded. Return order is not guaranteed.
 * The affected-row count returned by {@code execute} follows the database and
 * JDBC driver conventions and is not normalized across dialects.
 */
public interface MutableUpsert<S extends BaseTable>
        extends Executable<Integer>, ReturningSelectable {

    /**
     * Assign a value on insertion and include the target in the conflict key.
     * The key value is not assigned in the update branch. All {@code key} calls
     * together must cover one complete id or metadata-declared key group.
     * At least one key assignment is required.
     *
     * @param target a physical property of the mutation target
     * @param source the value to insert and use for conflict matching
     * @param <T> the value type
     * @return this command
     */
    @OldChain
    <T> MutableUpsert<S> key(PropExpression<T> target, Expression<T> source);

    /**
     * Assign a value only when a new row is inserted. A conflicting row retains
     * its existing value for this target.
     *
     * @param target a physical property of the mutation target
     * @param source the value to insert
     * @param <T> the value type
     * @return this command
     */
    @OldChain
    <T> MutableUpsert<S> insert(PropExpression<T> target, Expression<T> source);

    /**
     * Assign a physical scalar target only when a conflict is updated.
     * The expression can reference target and source values. On insertion,
     * the column retains its database default or framework initialization.
     * In particular, an update-only version still receives its initial value on
     * insertion. This method adds no implicit optimistic-lock check or increment.
     *
     * <p>The target must map to one column, including a scalar embedded member.
     * Id, logical-delete, and discriminator properties cannot be update-only targets.
     * Use {@link #merge(PropExpression, Expression, Expression)} to also specify
     * an insert value for the same target.
     *
     * @param target a physical scalar property of the mutation target
     * @param expression the value to assign to an accepted conflicting row
     * @param <T> the value type
     * @return this command
     */
    @OldChain
    <T> MutableUpsert<S> update(PropExpression<T> target, Expression<T> expression);

    /**
     * Assign the same source expression on insertion and on an accepted update.
     * Equivalent to {@code merge(target, source, source)}.
     *
     * @param target a physical property of the mutation target
     * @param source the value to assign in either branch
     * @param <T> the value type
     * @return this command
     */
    @OldChain
    <T> MutableUpsert<S> merge(PropExpression<T> target, Expression<T> source);

    /**
     * Assign different expressions in the insert and update branches.
     * The insert expression reads the source; the update expression can also read
     * the existing target row, for example to add a source amount to a stored total.
     *
     * @param target a physical property of the mutation target
     * @param insertSource the value to insert when no conflict exists
     * @param updateExpression the value to assign to an accepted conflicting row
     * @param <T> the value type
     * @return this command
     */
    @OldChain
    <T> MutableUpsert<S> merge(
            PropExpression<T> target,
            Expression<T> insertSource,
            Expression<T> updateExpression
    );

    /**
     * Add conditions that restrict only the matched update branch.
     * Conditions can read both source values and the existing target row.
     * Non-null predicates from all calls are combined with {@code AND}.
     *
     * <p>A conflicting row for which the condition is not true is left unchanged
     * and excluded from returning. A source row without a conflict is still
     * inserted. The conditions also apply when the command has no update
     * assignments and performs a self-assignment to preserve upsert semantics.
     *
     * @param predicates conditions to add; null elements are ignored
     * @return this command
     */
    @OldChain
    MutableUpsert<S> updateWhere(Predicate... predicates);
}
