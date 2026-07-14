package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.mutation.SaveAssignmentExpression;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.table.spi.UntypedJoinDisabledTableProxy;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

final class SaveAssignments {

    private static final String DISABLED_JOIN_REASON =
            "Joining is disabled for save assignment expressions";

    private SaveAssignments() {
    }

    static List<SaveAssignment> of(
            SaveContext ctx,
            Shape shape,
            ImmutableType tableType,
            List<PropertyGetter> targets
    ) {
        Map<ImmutableProp, SaveAssignmentLambda> lambdaMap = ctx.options.getAssignments();
        if (lambdaMap.isEmpty()) {
            return SaveAssignment.defaults(targets);
        }
        Set<ImmutableProp> matchedProps = new LinkedHashSet<>();
        List<SaveAssignment> assignments = new ArrayList<>(targets.size());
        for (PropertyGetter target : targets) {
            SaveAssignmentLambda lambda = lambdaMap.get(target.prop());
            if (lambda == null) {
                assignments.add(SaveAssignment.defaultOf(target));
            } else {
                matchedProps.add(target.prop());
                assignments.add(compile(ctx, shape, tableType, target, lambda));
            }
        }
        for (Map.Entry<ImmutableProp, SaveAssignmentLambda> e : lambdaMap.entrySet()) {
            ImmutableProp prop = e.getKey();
            if (e.getValue().type.isAssignableFrom(shape.getType()) &&
                    belongsToTable(prop, tableType) &&
                    !matchedProps.contains(prop)) {
                throw new IllegalArgumentException(
                        "The save assignment target \"" +
                                prop +
                                "\" is not selected for update by the current save shape or upsert mask"
                );
            }
        }
        return Collections.unmodifiableList(assignments);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SaveAssignment compile(
            SaveContext ctx,
            Shape shape,
            ImmutableType tableType,
            PropertyGetter target,
            SaveAssignmentLambda lambda
    ) {
        ImmutableProp targetProp = target.prop();
        ImmutableType lambdaType = lambda.type;
        if (lambdaType != targetProp.getDeclaringType()) {
            throw new IllegalArgumentException(
                    "The table type of the save assignment for \"" +
                            targetProp +
                            "\" does not match its declaring type"
            );
        }
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        MutableRootQueryImpl<?> fakeQuery = new MutableRootQueryImpl<>(
                sqlClient,
                lambdaType,
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
        Expression<?> value = ((SaveAssignmentExpression) lambda.expression).value(
                table,
                ValueExpressionFactories.of()
        );
        if (value == null) {
            throw new IllegalArgumentException(
                    "The save assignment expression for \"" + targetProp + "\" cannot be null"
            );
        }
        Class<?> valueType = ((ExpressionImplementor<?>) value).getType();
        if (!Classes.matches(targetProp.getReturnClass(), valueType)) {
            throw new IllegalArgumentException(
                    "The save assignment expression for \"" +
                            targetProp +
                            "\" has incompatible type \"" +
                            valueType.getName() +
                            "\""
            );
        }
        List<PropertyGetter> inputGetters = new ArrayList<>();
        ((Ast) value).accept(new AstVisitor(new AstContext(sqlClient)) {

            @Override
            public void visitTableReference(
                    RealTable table,
                    @Nullable ImmutableProp prop,
                    boolean rawId
            ) {
                if (prop == null || !belongsToTable(prop, tableType) || singleGetter(sqlClient, prop) == null) {
                    throw new IllegalArgumentException(
                            "The save assignment expression for \"" +
                                    targetProp +
                                    "\" can only read local physical scalar target properties"
                    );
                }
            }

            @Override
            public void visitSaveInputValue(ImmutableProp prop) {
                PropertyGetter getter = singleGetter(sqlClient, prop);
                if (getter == null || !belongsToTable(prop, tableType)) {
                    throw new IllegalArgumentException(
                            "The save assignment expression for \"" +
                                    targetProp +
                                    "\" can only read local physical scalar input properties"
                    );
                }
                if (!contains(shape.getGetters(), getter)) {
                    throw new IllegalArgumentException(
                            "The input property \"" +
                                    prop +
                                    "\" referenced by the save assignment for \"" +
                                    targetProp +
                                    "\" is unloaded"
                    );
                }
                if (!contains(inputGetters, getter)) {
                    inputGetters.add(getter);
                }
            }
        });
        return SaveAssignment.custom(target, value, inputGetters);
    }

    private static boolean belongsToTable(ImmutableProp prop, ImmutableType tableType) {
        ImmutableType declaringType = prop.getDeclaringType();
        if (declaringType == tableType) {
            return true;
        }
        InheritanceInfo inheritanceInfo = declaringType.getInheritanceInfo();
        return inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.SINGLE_TABLE &&
                inheritanceInfo.getRootType() == tableType;
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

    private static boolean contains(List<PropertyGetter> getters, PropertyGetter getter) {
        for (PropertyGetter existing : getters) {
            if (existing.equals(getter)) {
                return true;
            }
        }
        return false;
    }
}
