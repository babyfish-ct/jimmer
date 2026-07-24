package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableShape;

import java.util.List;

public interface BaseTableProjection<B extends BaseTable> {

    List<Selection<?>> getSelections();

    BaseTableShape<B, B> getBaseTableShape();
}
