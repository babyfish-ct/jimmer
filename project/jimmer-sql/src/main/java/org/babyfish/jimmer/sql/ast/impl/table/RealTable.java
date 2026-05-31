package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsageVisitor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface RealTable extends Iterable<RealTable> {

    TableLikeImplementor<?> getTableLikeImplementor();

    RealTable getParent();

    Key getKey();

    TableAliasKey getAliasKey();

    RealTable child(Key key);

    @Nullable
    BaseTableOwner getBaseTableOwner();

    void use(TableUsageVisitor visitor);

    void renderTo(@NotNull AbstractSqlBuilder<?> builder, boolean cte);

    void renderJoinAsFrom(SqlBuilder builder, TableImplementor.RenderMode mode);

    final class Key {

        final JoinTypeMergeScope scope;

        final WeakJoinHandle weakJoinHandle;

        final String joinName;

        Key(
                JoinTypeMergeScope scope,
                boolean inverse,
                ImmutableProp joinProp,
                WeakJoinHandle weakJoinHandle
        ) {
            this.scope = scope;
            this.weakJoinHandle = weakJoinHandle;
            String joinName;
            if (joinProp == null) {
                joinName = "";
            } else if (inverse) {
                ImmutableProp opposite = joinProp.getOpposite();
                if (opposite != null) {
                    joinName = opposite.getName();
                } else {
                    joinName = "←" + joinProp.getName();
                }
            } else {
                joinName = joinProp.getName();
            }
            this.joinName = joinName;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(scope);
            result = 31 * result + joinName.hashCode();
            result = 31 * result + Objects.hashCode(weakJoinHandle);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key other = (Key) o;
            if (scope != other.scope) {
                return false;
            }
            if (!joinName.equals(other.joinName)) {
                return false;
            }
            return Objects.equals(weakJoinHandle, other.weakJoinHandle);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "scope=" + scope +
                    ", joinName=" + joinName +
                    ", weakJoinHandle=" + weakJoinHandle +
                    "}";
        }
    }
}
