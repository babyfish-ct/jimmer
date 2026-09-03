package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpsert;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.ArrayList;
import java.util.List;

public final class MutableUpsertImpl<S extends BaseTable>
        extends AbstractInsertFromSelectImpl<S>
        implements MutableUpsert<S> {

    private List<Target> keyTargets;

    public MutableUpsertImpl(JSqlClientImplementor sqlClient, TableProxy<?> target, S source) {
        super(sqlClient, target, source);
    }

    public MutableUpsertImpl(JSqlClientImplementor sqlClient, ImmutableType targetType, S source) {
        super(sqlClient, targetType, source);
    }

    @Override
    public <T> MutableUpsert<S> key(PropExpression<T> target, Expression<T> source) {
        addAssignment(target, source, null, Role.KEY);
        return this;
    }

    @Override
    public <T> MutableUpsert<S> insert(PropExpression<T> target, Expression<T> source) {
        addAssignment(target, source, null, Role.INSERT);
        return this;
    }

    @Override
    public <T> MutableUpsert<S> merge(PropExpression<T> target, Expression<T> source) {
        addAssignment(target, source, source, Role.MERGE);
        return this;
    }

    @Override
    public <T> MutableUpsert<S> merge(
            PropExpression<T> target,
            Expression<T> insertSource,
            Expression<T> updateExpression
    ) {
        addAssignment(target, insertSource, updateExpression, Role.MERGE);
        return this;
    }

    @Override
    public MutableUpsert<S> updateWhere(Predicate... predicates) {
        addUpdatePredicates(predicates);
        return this;
    }

    @Override
    void validateSemantics() {
        List<Target> targets = new ArrayList<>();
        for (Assignment assignment : assignments.values()) {
            if (assignment.role == Role.KEY) {
                targets.add(assignment.target);
            }
        }
        if (targets.isEmpty()) {
            throw new IllegalStateException("At least one key assignment is required for upsert");
        }
        keyTargets = validateUniqueTargets(targets, "upsert key");
    }

    @Override
    boolean isUpsert() {
        return true;
    }

    @Override
    boolean isConflictIgnored() {
        return false;
    }

    @Override
    List<Target> conflictTargets() {
        return keyTargets;
    }
}
