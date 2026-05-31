package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TableAliasKey {

    private final AbstractMutableStatementImpl statement;

    private final List<RealTable.Key> path;

    private final int hashCode;

    private TableAliasKey(AbstractMutableStatementImpl statement, List<RealTable.Key> path) {
        this.statement = statement;
        this.path = path;
        this.hashCode = 31 * System.identityHashCode(statement) + path.hashCode();
    }

    static TableAliasKey of(RealTable table) {
        List<RealTable.Key> path = new ArrayList<>();
        for (RealTable t = table; t != null; t = t.getParent()) {
            path.add(t.getKey());
        }
        Collections.reverse(path);
        return new TableAliasKey(
                table.getTableLikeImplementor().getStatement(),
                Collections.unmodifiableList(path)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TableAliasKey)) {
            return false;
        }
        TableAliasKey other = (TableAliasKey) o;
        return statement == other.statement && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
