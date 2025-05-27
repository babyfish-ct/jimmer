package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AstContext;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BaseSelectionMapper {

    private final BaseQueryScope scope;

    final Map<String, Integer> indexMap = new LinkedHashMap<>();

    int expressionIndex;

    public BaseSelectionMapper(BaseQueryScope scope) {
        this.scope = scope;
    }

    public String getAlias(AstContext ctx) {
        return scope.table().realTable(ctx.getJoinTypeMergeScope()).getAlias();
    }

    public int columnIndex(String alias, String columnName) {
        return indexMap.computeIfAbsent(alias + '.' + columnName, it -> scope.colNo());
    }

    public int expressionIndex() {
        if (expressionIndex == 0) {
            expressionIndex = scope.colNo();
        }
        return expressionIndex;
    }
}
