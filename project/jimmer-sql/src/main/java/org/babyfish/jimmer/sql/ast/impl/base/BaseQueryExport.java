package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BaseQueryExport {

    private final BaseQueryScope scope;

    private final RealTable realBaseTable;

    private final Map<Integer, BaseSelectionMapper> selectionMapperMap =
            new LinkedHashMap<>();

    BaseQueryExport(BaseQueryScope scope, RealTable realBaseTable) {
        this.scope = scope;
        this.realBaseTable = realBaseTable;
    }

    public RealTable getRealBaseTable() {
        return realBaseTable;
    }

    public BaseSelectionMapper mapper(int selectionIndex) {
        return selectionMapperMap.computeIfAbsent(
                selectionIndex,
                it -> new BaseSelectionMapper(this, it)
        );
    }

    public BaseSelectionMapper mapperOrNull(int selectionIndex) {
        return selectionMapperMap.get(selectionIndex);
    }

    AstContext astContext() {
        return scope.astContext;
    }

    int nextColumnIndex() {
        return scope.colNo();
    }
}
