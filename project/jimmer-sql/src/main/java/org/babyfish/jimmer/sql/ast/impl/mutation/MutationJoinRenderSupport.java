package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.babyfish.jimmer.sql.runtime.TableUsedState;

final class MutationJoinRenderSupport {

    private MutationJoinRenderSupport() {}

    static boolean hasUsedChild(TableImplementor<?> table, AstContext astContext) {
        for (RealTable childTable : table.realTable(astContext)) {
            if (astContext.getTableUsedState(childTable) == TableUsedState.USED) {
                return true;
            }
        }
        return false;
    }

    static boolean hasFirstLevelJoinUnsupportedByFromOnly(TableImplementor<?> table, AstContext astContext) {
        for (RealTable childTable : table.realTable(astContext)) {
            if (astContext.getTableUsedState(childTable) != TableUsedState.USED) {
                continue;
            }
            TableLikeImplementor<?> implementor = childTable.getTableLikeImplementor();
            if (implementor instanceof TableImplementor<?>) {
                TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
                if (tableImplementor.getJoinType() != JoinType.INNER ||
                        tableImplementor.getWeakJoinHandle() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    static void renderUsedJoinsAsFrom(SqlBuilder builder, TableImplementor<?> table) {
        for (RealTable child : table.realTable(builder.getAstContext())) {
            child.renderJoinAsFrom(builder, TableImplementor.RenderMode.FROM_ONLY);
        }
    }

    static void renderUsedJoinsNormally(SqlBuilder builder, TableImplementor<?> table) {
        for (RealTable child : table.realTable(builder.getAstContext())) {
            child.renderTo(builder, false);
        }
    }

    static void renderDeeperJoinsAsFrom(SqlBuilder builder, TableImplementor<?> table) {
        for (RealTable child : table.realTable(builder.getAstContext())) {
            child.renderJoinAsFrom(builder, TableImplementor.RenderMode.DEEPER_JOIN_ONLY);
        }
    }

    static void renderUsedJoinConditions(SqlBuilder builder, TableImplementor<?> table) {
        for (RealTable child : table.realTable(builder.getAstContext())) {
            child.renderJoinAsFrom(builder, TableImplementor.RenderMode.WHERE_ONLY);
        }
    }
}
