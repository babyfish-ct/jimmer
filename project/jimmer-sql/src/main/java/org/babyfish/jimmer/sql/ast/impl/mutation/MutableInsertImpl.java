package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.mutation.MutableInsert;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MutableInsertImpl<S extends BaseTable>
        extends AbstractInsertFromSelectImpl<S>
        implements MutableInsert<S> {

    private boolean conflictIgnored;

    private boolean inferConflictTargets;

    private List<Target> explicitConflictTargets;

    private List<Target> resolvedConflictTargets;

    public MutableInsertImpl(JSqlClientImplementor sqlClient, TableProxy<?> target, S source) {
        super(sqlClient, target, source);
    }

    public MutableInsertImpl(JSqlClientImplementor sqlClient, ImmutableType targetType, S source) {
        super(sqlClient, targetType, source);
    }

    @Override
    public <T> MutableInsert<S> set(PropExpression<T> target, Expression<T> source) {
        addAssignment(target, source, null, Role.INSERT);
        return this;
    }

    @Override
    public MutableInsert<S> onConflictDoNothing() {
        validateMutable();
        conflictIgnored = true;
        inferConflictTargets = true;
        explicitConflictTargets = null;
        resolvedConflictTargets = null;
        return this;
    }

    @Override
    public MutableInsert<S> onConflictDoNothing(PropExpression<?>... targetProps) {
        validateMutable();
        if (targetProps == null || targetProps.length == 0) {
            throw new IllegalArgumentException(
                    "Explicit conflict properties cannot be empty; use onConflictDoNothing() for inference"
            );
        }
        List<Target> targets = new ArrayList<>(targetProps.length);
        for (PropExpression<?> targetProp : targetProps) {
            Target target = Target.of(targetProp, getSqlClient().getMetadataStrategy());
            validateTargetForConflict(target);
            targets.add(target);
        }
        conflictIgnored = true;
        inferConflictTargets = false;
        explicitConflictTargets = targets;
        resolvedConflictTargets = null;
        return this;
    }

    private void validateTargetForConflict(Target target) {
        if (!org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable.__refEquals(getTable(), target.table)) {
            throw new IllegalArgumentException(
                    "Conflict property \"" + target.prop + "\" does not belong to the mutation target"
            );
        }
        if (!target.prop.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "Conflict property \"" + target.prop + "\" is not backed by physical columns"
            );
        }
    }

    @Override
    void validateSemantics() {
        if (!conflictIgnored) {
            return;
        }
        resolvedConflictTargets = inferConflictTargets ?
                inferConflictTargets() :
                validateUniqueTargets(explicitConflictTargets, "conflict target");
    }

    @Override
    boolean isUpsert() {
        return false;
    }

    @Override
    boolean isConflictIgnored() {
        return conflictIgnored;
    }

    @Override
    List<Target> conflictTargets() {
        return resolvedConflictTargets != null ? resolvedConflictTargets : Collections.emptyList();
    }
}
