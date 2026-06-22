package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class CteTableDependencies {

    static final CteTableDependencies EMPTY = new CteTableDependencies(
            Collections.emptyMap(),
            Collections.emptyList()
    );

    private final Map<AbstractMutableStatementImpl, List<RealTable>> renderTableMap;

    private final List<RealTable> aliasRootTables;

    CteTableDependencies(
            Map<AbstractMutableStatementImpl, List<RealTable>> renderTableMap,
            List<RealTable> aliasRootTables
    ) {
        this.renderTableMap = renderTableMap;
        this.aliasRootTables = aliasRootTables;
    }

    List<RealTable> renderTables(AbstractMutableStatementImpl statement) {
        List<RealTable> tables = renderTableMap.get(statement);
        return tables != null ? tables : Collections.emptyList();
    }

    List<RealTable> aliasRootTables() {
        return aliasRootTables;
    }
}
