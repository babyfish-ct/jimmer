package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public interface BaseSelectionAliasRender {

    void render(int index, Selection<?> selection, SqlBuilder builder);

    void renderCteColumns(RealTable realTable, SqlBuilder builder);
}
