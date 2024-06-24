package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collection;
import java.util.List;

class Deleter {

    private final Deleter parent;

    private final DeleteContext ctx;

    private final Collection<Object> deletedIds;

    private final ImmutableProp backReferenceProp;

    private final IdPairs retainedIdPairs;

    private final List<ValueGetter> sourceGetters;

    private final List<ValueGetter> targetGetters;

    Deleter(Deleter parent, DeleteContext ctx, Collection<Object> deletedIds, IdPairs retainedIdPairs) {
        this.parent = parent;
        this.ctx = ctx;
        this.deletedIds = deletedIds;
        this.retainedIdPairs = retainedIdPairs;
        ImmutableProp mappedBy = ctx.path.getProp().getMappedBy();
        this.backReferenceProp = mappedBy != null && mappedBy.isReference(TargetLevel.ENTITY) ?
                mappedBy :
                null;
        if (backReferenceProp != null) {
            this.sourceGetters = ValueGetter.valueGetters(
                    ctx.options.getSqlClient(),
                    backReferenceProp
            );
        } else {
            this.sourceGetters = null;
        }
        this.targetGetters = ValueGetter.valueGetters(
                ctx.options.getSqlClient(),
                ctx.path.getType().getIdProp()
        );
    }

    public void execute() {

    }

    private void addPredicates(BatchSqlBuilder builder) {
        builder.separator();
        if (retainedIdPairs != null) {
            ExclusiveIdPairPredicates.addPredicates(
                    builder,
                    sourceGetters,
                    targetGetters
            );
            return;
        }
        if (deletedIds != null) {
            builder.enter(
                    targetGetters.size() == 1 ?
                            AbstractSqlBuilder.ScopeType.NULL :
                            AbstractSqlBuilder.ScopeType.AND
            );
            for (ValueGetter targetGetter : targetGetters) {
                builder.separator();
                builder.sql(targetGetter)
                        .sql(targetGetter).sql(" = ")
                        .variable(targetGetter);
            }
            builder.leave();
            return;
        }
        builder.enter(
                sourceGetters.size() == 1 ?
                        AbstractSqlBuilder.ScopeType.NULL :
                        AbstractSqlBuilder.ScopeType.TUPLE
        );
        for (ValueGetter sourceGetter : sourceGetters) {
            builder.separator();
            builder.sql(sourceGetter);
        }
        builder.sql(" in ").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
        builder.enter(AbstractSqlBuilder.ScopeType.SELECT);
        List<ValueGetter> parentGetters = ValueGetter.valueGetters(
                builder.sqlClient(),
                parent.ctx.path.getType().getIdProp()
        );
        for (ValueGetter parentGetter : parentGetters) {
            builder.separator().sql(parentGetter);
        }
        builder.leave();
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        parent.addPredicates(builder);
        builder.leave();
        builder.leave();
        builder.leave();
    }

    private void addPredicates(SqlBuilder builder) {
        builder.separator();
        if (retainedIdPairs != null) {
            ExclusiveIdPairPredicates.addPredicates(
                    builder,
                    sourceGetters,
                    targetGetters,
                    retainedIdPairs
            );
            return;
        }
        if (deletedIds != null) {
            ComparisonPredicates.renderIn(
                    false,
                    targetGetters,
                    deletedIds,
                    builder
            );
            return;
        }
        builder.enter(
                sourceGetters.size() == 1 ?
                        AbstractSqlBuilder.ScopeType.NULL :
                        AbstractSqlBuilder.ScopeType.TUPLE
        );
        for (ValueGetter sourceGetter : sourceGetters) {
            builder.separator();
            builder.sql(sourceGetter);
        }
        builder.sql(" in ").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
        builder.enter(AbstractSqlBuilder.ScopeType.SELECT);
        List<ValueGetter> parentGetters = ValueGetter.valueGetters(
                builder.sqlClient(),
                parent.ctx.path.getType().getIdProp()
        );
        for (ValueGetter parentGetter : parentGetters) {
            builder.separator().sql(parentGetter);
        }
        builder.leave();
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        parent.addPredicates(builder);
        builder.leave();
        builder.leave();
        builder.leave();
    }

    private boolean isBatchAllowed() {
        if (retainedIdPairs != null) {
            return retainedIdPairs.entries().size() < 2 &&
                    ctx.options.getSqlClient().getDialect().isAnyEqualityOfArraySupported();
        }
        if (deletedIds != null) {
            return true;
        }
        return parent.isBatchAllowed();
    }
}
