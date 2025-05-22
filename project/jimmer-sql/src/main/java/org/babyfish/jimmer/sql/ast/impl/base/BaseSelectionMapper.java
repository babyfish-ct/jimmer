package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.AstContext;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BaseSelectionMapper {

    private final BaseQueryScope scope;

    private final BaseTableOwner baseTableOwner;

    private final BaseSelectionMapper parent;

    final Map<String, Integer> indexMap = new LinkedHashMap<>();

    int expressionIndex;

    public BaseSelectionMapper(BaseQueryScope scope, BaseTableOwner baseTableOwner, BaseSelectionMapper parent) {
        this.scope = scope;
        this.baseTableOwner = baseTableOwner;
        this.parent = parent;
    }

    public String getAlias(AstContext ctx) {
        return scope.table().realTable(ctx.getJoinTypeMergeScope()).getAlias();
    }

    public int columnIndex(String columnName) {
        return indexMap.computeIfAbsent(columnName, it -> scope.colNo());
    }

    public int expressionIndex() {
        if (expressionIndex == 0) {
            expressionIndex = scope.colNo();
        }
        return expressionIndex;
    }

    public BaseTableOwner getBaseTableOwner() {
        return baseTableOwner;
    }

    public BaseSelectionMapper getParent() {
        return parent;
    }
}
