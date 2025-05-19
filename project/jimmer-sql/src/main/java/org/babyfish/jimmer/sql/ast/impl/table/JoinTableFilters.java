package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.ast.impl.Variables;
import org.babyfish.jimmer.sql.meta.JoinTableFilterInfo;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public class JoinTableFilters {

    private JoinTableFilters() {
    }

    public static void render(LogicalDeletedBehavior behavior, LogicalDeletedInfo info, String alias, SqlBuilder builder) {
        if (behavior == LogicalDeletedBehavior.IGNORED) {
            return;
        }
        boolean isInverse = behavior == LogicalDeletedBehavior.REVERSED;
        if (alias != null) {
            builder.sql(alias).sql(".");
        }
        builder.sql(info.getColumnName());
        LogicalDeletedInfo.Action action = info.getAction();
        if (action instanceof LogicalDeletedInfo.Action.Eq) {
            LogicalDeletedInfo.Action.Eq eq = (LogicalDeletedInfo.Action.Eq) action;
            Object value = eq.getValue();
            value = Variables.process(value, info.getType(), builder.sqlClient());
            builder.sql(isInverse ? " <> " : " = ").rawVariable(value);
        } else if (action instanceof LogicalDeletedInfo.Action.Ne) {
            LogicalDeletedInfo.Action.Ne ne = (LogicalDeletedInfo.Action.Ne) action;
            Object value = ne.getValue();
            value = Variables.process(value, info.getType(), builder.sqlClient());
            builder.sql(isInverse ? " = " : " <> ").rawVariable(value);
        } else if (action instanceof LogicalDeletedInfo.Action.IsNull) {
            builder.sql(" is null");
        } else if (action instanceof LogicalDeletedInfo.Action.IsNotNull) {
            builder.sql(" is not null");
        }
    }

    public static void render(JoinTableFilterInfo info, String alias, SqlBuilder builder) {
        if (alias != null) {
            builder.sql(alias).sql(".");
        }
        builder.sql(info.getColumnName());
        if (info.getValues().size() == 1) {
            builder.sql(" = ").variable(info.getValues().iterator().next());
        } else {
            boolean addComma = false;
            builder.sql(" in (");
            for (Object value : info.getValues()) {
                if (addComma) {
                    builder.sql(", ");
                } else {
                    addComma = true;
                }
                builder.variable(value);
            }
            builder.sql(")");
        }
    }
}
