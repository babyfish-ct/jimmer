package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.mutation.SaveAssignmentExpression;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

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
                    SaveExpressionUtils.belongsToTable(prop, tableType) &&
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
        Table<?> table = SaveExpressionUtils.table(sqlClient, lambdaType, DISABLED_JOIN_REASON);
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
        List<PropertyGetter> inputGetters = SaveExpressionUtils.validate(
                (Ast) value,
                sqlClient,
                tableType,
                shape,
                "The save assignment expression for \"" + targetProp + "\"",
                "the save assignment for \"" + targetProp + "\""
        );
        return SaveAssignment.custom(target, value, inputGetters);
    }
}
