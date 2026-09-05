package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.UpdateCondition;
import org.babyfish.jimmer.sql.ast.mutation.ValueExpressionFactory;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SaveUpdateConditions {

    private static final String DISABLED_JOIN_REASON =
            "Joining is disabled for save update-where predicates";

    private SaveUpdateConditions() {
    }

    static boolean validate(SaveContext ctx, List<DraftSpi> drafts) {
        if (drafts.isEmpty()) {
            return false;
        }
        ImmutableType type = ctx.path.getType();
        if (ctx.path.getParent() == null) {
            validateConfiguredTypes(ctx.options, type);
        }
        TypedUpdateCondition typedCondition = ctx.options.getUpdateWhere(type);
        if (typedCondition == null) {
            return false;
        }
        Set<Shape> shapes = new LinkedHashSet<>();
        for (DraftSpi draft : drafts) {
            shapes.add(Shape.of(ctx.options.getSqlClient(), draft, null));
        }
        boolean active = false;
        for (Shape shape : shapes) {
            Predicate predicate = validate(ctx, shape, typedCondition);
            if (predicate != null) {
                if (ctx.updateWherePredicate == null) {
                    ctx.updateWherePredicate = predicate;
                }
                active = true;
            }
        }
        return active;
    }

    private static void validateConfiguredTypes(SaveOptions options, ImmutableType savedType) {
        ImmutableType savedFamily = familyType(savedType);
        for (ImmutableType configuredType : options.getUpdateWheres().keySet()) {
            if (familyType(configuredType) != savedFamily) {
                throw new IllegalArgumentException(
                        "The update-where table type \"" + configuredType +
                                "\" is unrelated to the saved entity type \"" + savedType + "\""
                );
            }
        }
    }

    private static ImmutableType familyType(ImmutableType type) {
        ImmutableType rootType = type.getInheritanceRoot();
        return rootType != null ? rootType : type;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable Predicate validate(
            SaveContext ctx,
            Shape shape,
            TypedUpdateCondition typedCondition
    ) {
        ImmutableType tableType = SaveExpressionUtils.physicalTableType(shape.getType());
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        Table<?> table = SaveExpressionUtils.table(sqlClient, typedCondition.type, DISABLED_JOIN_REASON);
        Predicate predicate = ((UpdateCondition) typedCondition.condition).predicate(
                table,
                ValueExpressionFactories.of()
        );
        if (predicate == null) {
            return null;
        }
        SaveExpressionUtils.validate(
                (Ast) predicate,
                sqlClient,
                tableType,
                shape,
                "A save update-where predicate",
                "the save update-where predicate"
        );
        return predicate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean isAllowed(SaveContext ctx, DraftSpi draft) {
        TypedUpdateCondition typedCondition = ctx.options.getUpdateWhere(ctx.path.getType());
        if (typedCondition == null) {
            return true;
        }
        ImmutableType type = ctx.path.getType();
        Object id = draft.__get(type.getIdProp().getId());
        List<?> ids = Queries.createQuery(
                ctx.options.getSqlClient(),
                type,
                ExecutionPurpose.command(QueryReason.UPDATE_WHERE),
                FilterLevel.IGNORE_USER_FILTERS,
                (q, table) -> {
                    q.where(table.get(type.getIdProp()).eq(id));
                    Predicate predicate = ((UpdateCondition) typedCondition.condition).predicate(
                            table,
                            new DraftValueExpressionFactory(draft)
                    );
                    if (predicate == null) {
                        return q.select(table.get(type.getIdProp()));
                    }
                    q.where(predicate);
                    return q.select(table.get(type.getIdProp()));
                }
        ).forUpdate(ctx.options.isPessimisticLocked(type)).execute(ctx.con);
        return !ids.isEmpty();
    }

    private static final class DraftValueExpressionFactory implements ValueExpressionFactory<Object> {

        private final DraftSpi draft;

        private DraftValueExpressionFactory(DraftSpi draft) {
            this.draft = draft;
        }

        @Override
        public <V> Expression<V> newValue(org.babyfish.jimmer.meta.TypedProp.Scalar<Object, V> prop) {
            return newValue(prop.unwrap());
        }

        @Override
        public StringExpression newString(org.babyfish.jimmer.meta.TypedProp.Scalar<Object, String> prop) {
            String value = (String) value(prop.unwrap());
            return value != null ? Literals.string(value) : Expression.string().sql("null");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <N extends Number & Comparable<N>> NumericExpression<N> newNumber(
                org.babyfish.jimmer.meta.TypedProp.Scalar<Object, N> prop
        ) {
            N value = (N) value(prop.unwrap());
            return value != null ?
                    Literals.number(value) :
                    Expression.numeric().sql((Class<N>) Classes.boxTypeOf(prop.unwrap().getReturnClass()), "null");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <C extends Comparable<?>> ComparableExpression<C> newComparable(
                org.babyfish.jimmer.meta.TypedProp.Scalar<Object, C> prop
        ) {
            C value = (C) value(prop.unwrap());
            return value != null ?
                    Literals.comparable(value) :
                    Expression.comparable().sql((Class<C>) Classes.boxTypeOf(prop.unwrap().getReturnClass()), "null");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> Expression<V> newValue(ImmutableProp prop) {
            Object value = value(prop);
            return value != null ?
                    (Expression<V>) Literals.any(value) :
                    Expression.any().sql((Class<V>) Classes.boxTypeOf(prop.getReturnClass()), "null");
        }

        private Object value(ImmutableProp prop) {
            if (prop.isDiscriminator()) {
                return ImmutableObjects.getDiscriminator(draft);
            }
            return draft.__get(prop.getId());
        }
    }
}
