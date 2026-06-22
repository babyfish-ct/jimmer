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

    private final Map<AbstractMutableStatementImpl, List<CteTableDeclaration>> declarationMap;

    private final List<RealTable> aliasRootTables;

    CteTableDependencies(
            Map<AbstractMutableStatementImpl, List<CteTableDeclaration>> declarationMap,
            List<RealTable> aliasRootTables
    ) {
        this.declarationMap = declarationMap;
        this.aliasRootTables = aliasRootTables;
    }

    List<CteTableDeclaration> declarations(AbstractMutableStatementImpl statement) {
        List<CteTableDeclaration> declarations = declarationMap.get(statement);
        return declarations != null ? declarations : Collections.emptyList();
    }

    List<RealTable> aliasRootTables() {
        return aliasRootTables;
    }
}
