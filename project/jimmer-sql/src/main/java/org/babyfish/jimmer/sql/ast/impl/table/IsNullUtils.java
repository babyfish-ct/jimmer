package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class IsNullUtils {

    private IsNullUtils() {}

    public static void isValidIsNullExpression(@NotNull PropExpressionImplementor<?> propExpression) {
        for (PropExpressionImplementor<?> pe = propExpression; pe != null; pe = pe.getBase()) {
            if (pe.getProp().isNullable()) {
                return;
            }
        }
        for (Table<?> table = propExpression.getTable(); table != null; ) {
            if (table instanceof TableProxy<?>) {
                TableProxy<?> proxy = (TableProxy<?>) table;
                ImmutableProp prop = proxy.__prop();
                if (proxy.__isInverse()) {
                    prop = prop.getOpposite();
                }
                if (prop != null) {
                    JoinType currentJoinType = proxy.__joinType();
                    if (currentJoinType == JoinType.LEFT || currentJoinType == JoinType.FULL) {
                        return;
                    }
                }
                table = proxy.__parent();
            } else {
                TableImplementor<?> impl = (TableImplementor<?>) table;
                ImmutableProp prop = impl.getJoinProp();
                if (impl.isInverse()) {
                    prop = prop.getOpposite();
                }
                if (prop != null) {
                    JoinType currentJoinType = impl.getJoinType();
                    if (currentJoinType == JoinType.LEFT || currentJoinType == JoinType.FULL) {
                        return;
                    }
                }
                table = impl.getParent();
            }
        }

        List<String> pathNames = new LinkedList<>();
        for (PropExpressionImplementor<?> pe = propExpression; pe != null; pe = pe.getBase()) {
            if (pe.getProp().isNullable()) {
                pathNames.add(0, pe.getProp().getName());
            }
        }
        boolean joined = false;
        for (Table<?> table = propExpression.getTable(); table != null; ) {
            if (table instanceof TableProxy<?>) {
                TableProxy<?> proxy = (TableProxy<?>) table;
                ImmutableProp prop = proxy.__prop();
                if (proxy.__isInverse()) {
                    prop = prop.getOpposite();
                }
                if (prop != null) {
                    pathNames.add(0, prop.getName());
                } else if (proxy.__weakJoinHandle() != null) {
                    pathNames.add(
                            0,
                            "weakJoin<" + proxy.__weakJoinHandle().getWeakJoinType().getSimpleName() + ">"
                    );
                } else {
                    pathNames.add(0, table.getImmutableType().getJavaClass().getSimpleName());
                }
                table = proxy.__parent();
            } else {
                TableImplementor<?> impl = (TableImplementor<?>) table;
                ImmutableProp prop = impl.getJoinProp();
                if (impl.isInverse()) {
                    prop = prop.getOpposite();
                }
                if (prop != null) {
                    pathNames.add(0, prop.getName());
                } if (impl.getWeakJoinHandle() != null) {
                    pathNames.add(
                            0,
                            "weakJoin<" + impl.getWeakJoinHandle().getWeakJoinType().getSimpleName() + ">"
                    );
                } else {
                    pathNames.add(0, table.getImmutableType().getJavaClass().getSimpleName());
                }
                table = impl.getParent();
            }
        }
        String path = String.join(".", pathNames);
        if (!joined) {
            throw new IllegalArgumentException(
                    "Unable to instantiate the \"is null\" predicate, the path \"" +
                            path +
                            "\" is non-null expression"
            );
        }
        throw new IllegalArgumentException(
                "Unable to instantiate the \"is null\" predicate, the path \"" +
                        path +
                        "\" is neither non-null expression " +
                        "nor path with left or full table join"
        );
    }
}
