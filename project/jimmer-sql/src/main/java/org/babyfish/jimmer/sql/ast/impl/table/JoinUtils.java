package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

public class JoinUtils {

    private JoinUtils() {}

    public static boolean hasLeftJoin(Table<?> table) {
        while (table != null) {
            if (table instanceof TableProxy<?>) {
                TableProxy<?> proxy = (TableProxy<?>) table;
                ImmutableProp prop = proxy.__prop();
                if (proxy.__isInverse()) {
                    prop = prop.getOpposite();
                }
                if (prop != null && prop.isNullable()) {
                    JoinType currentJoinType = proxy.__currentJoinType();
                    if (currentJoinType == JoinType.LEFT || currentJoinType == JoinType.FULL) {
                        return true;
                    }
                }
                table = proxy.__parent();
            } else {
                TableImplementor<?> impl = (TableImplementor<?>) table;
                ImmutableProp prop = impl.getJoinProp();
                if (impl.isInverse()) {
                    prop = prop.getOpposite();
                }
                if (prop != null && prop.isNullable()) {
                    JoinType currentJoinType = impl.getCurrentJoinType();
                    if (currentJoinType == JoinType.LEFT || currentJoinType == JoinType.FULL) {
                        return true;
                    }
                }
                table = impl.getParent();
            }
        }
        return false;
    }
}
