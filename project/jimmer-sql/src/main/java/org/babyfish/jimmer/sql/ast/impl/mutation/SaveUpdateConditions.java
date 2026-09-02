package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.UpdateCondition;
import org.babyfish.jimmer.sql.ast.mutation.ValueExpressionFactory;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
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
        ImmutableType tableType = physicalTableType(shape.getType());
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        MutableRootQueryImpl<?> fakeQuery = new MutableRootQueryImpl<>(
                sqlClient,
                typedCondition.type,
                ExecutionPurpose.MUTATE,
                FilterLevel.DEFAULT
        );
        Table<?> table = fakeQuery.getTable();
        if (table instanceof TableImplementor<?>) {
            table = new UntypedJoinDisabledTableProxy<>(
                    (TableImplementor<?>) table,
                    DISABLED_JOIN_REASON
            );
        } else {
            table = ((TableProxy<?>) table).__disableJoin(DISABLED_JOIN_REASON);
        }
        Predicate predicate = ((UpdateCondition) typedCondition.condition).predicate(
                table,
                ValueExpressionFactories.of()
        );
        if (predicate == null) {
            return null;
        }
        ((Ast) predicate).accept(new AstVisitor(new AstContext(sqlClient)) {

            @Override
            public void visitTableReference(
                    RealTable table,
                    @Nullable ImmutableProp prop,
                    boolean rawId
            ) {
                if (prop == null || !belongsToTable(prop, tableType) || singleGetter(sqlClient, prop) == null) {
                    throw new IllegalArgumentException(
                            "A save update-where predicate can only read local physical scalar target properties"
                    );
                }
            }

            @Override
            public void visitSaveInputValue(ImmutableProp prop) {
                PropertyGetter getter = singleGetter(sqlClient, prop);
                if (getter == null || !belongsToTable(prop, tableType)) {
                    throw new IllegalArgumentException(
                            "A save update-where predicate can only read local physical scalar input properties"
                    );
                }
                if (!shape.contains(getter)) {
                    throw new IllegalArgumentException(
                            "The input property \"" + prop +
                                    "\" referenced by the save update-where predicate is unloaded"
                    );
                }
            }
        });
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
                    q.where(((Expression) ((Table<?>) table).get(type.getIdProp())).eq(id));
                    Predicate predicate = ((UpdateCondition) typedCondition.condition).predicate(
                            table,
                            new DraftValueExpressionFactory(draft)
                    );
                    if (predicate == null) {
                        return q.select(((Table<?>) table).get(type.getIdProp()));
                    }
                    q.where(predicate);
                    return q.select(((Table<?>) table).get(type.getIdProp()));
                }
        ).forUpdate(ctx.options.isPessimisticLocked(type)).execute(ctx.con);
        return !ids.isEmpty();
    }

    private static ImmutableType physicalTableType(ImmutableType type) {
        InheritanceInfo info = type.getInheritanceInfo();
        if (info != null && info.getStrategy() == InheritanceType.SINGLE_TABLE) {
            return info.getRootType();
        }
        return type;
    }

    private static boolean belongsToTable(ImmutableProp prop, ImmutableType tableType) {
        ImmutableType declaringType = prop.getDeclaringType();
        if (declaringType == tableType) {
            return true;
        }
        InheritanceInfo info = declaringType.getInheritanceInfo();
        return info != null &&
                info.getStrategy() == InheritanceType.SINGLE_TABLE &&
                info.getRootType() == tableType;
    }

    private static @Nullable PropertyGetter singleGetter(
            JSqlClientImplementor sqlClient,
            ImmutableProp prop
    ) {
        List<PropertyGetter> getters = PropertyGetter.propertyGetters(sqlClient, prop);
        if (getters.size() != 1 || getters.get(0).metadata().getColumnName() == null) {
            return null;
        }
        return getters.get(0);
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
            return Literals.string((String) value(prop.unwrap()));
        }

        @Override
        public <N extends Number & Comparable<N>> NumericExpression<N> newNumber(
                org.babyfish.jimmer.meta.TypedProp.Scalar<Object, N> prop
        ) {
            return Literals.number((N) value(prop.unwrap()));
        }

        @Override
        public <C extends Comparable<?>> ComparableExpression<C> newComparable(
                org.babyfish.jimmer.meta.TypedProp.Scalar<Object, C> prop
        ) {
            return Literals.comparable((C) value(prop.unwrap()));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> Expression<V> newValue(ImmutableProp prop) {
            Object value = value(prop);
            Expression<V> expression = value != null ?
                    (Expression<V>) Literals.any(value) :
                    Expression.nullValue((Class<V>) Classes.boxTypeOf(prop.getReturnClass()));
            return expression;
        }

        private Object value(ImmutableProp prop) {
            if (prop.isDiscriminator()) {
                return ImmutableObjects.getDiscriminator(draft);
            }
            return draft.__get(prop.getId());
        }
    }
}
