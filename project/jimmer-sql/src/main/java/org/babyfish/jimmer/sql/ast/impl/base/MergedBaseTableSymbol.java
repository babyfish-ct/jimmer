package org.babyfish.jimmer.sql.ast.impl.base;

import java.util.Set;

public interface MergedBaseTableSymbol extends BaseTableSymbol {

    Set<BaseTableSymbol> getBaseTables();
}
