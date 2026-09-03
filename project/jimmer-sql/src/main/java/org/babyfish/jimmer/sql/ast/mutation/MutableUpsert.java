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
 */
public interface MutableUpsert<S extends BaseTable>
        extends Executable<Integer>, ReturningSelectable {

    @OldChain
    <T> MutableUpsert<S> key(PropExpression<T> target, Expression<T> source);

    @OldChain
    <T> MutableUpsert<S> insert(PropExpression<T> target, Expression<T> source);

    @OldChain
    <T> MutableUpsert<S> merge(PropExpression<T> target, Expression<T> source);

    @OldChain
    <T> MutableUpsert<S> merge(
            PropExpression<T> target,
            Expression<T> insertSource,
            Expression<T> updateExpression
    );

    @OldChain
    MutableUpsert<S> updateWhere(Predicate... predicates);
}
