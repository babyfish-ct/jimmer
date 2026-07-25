package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.BaseTableFactory;

import java.util.List;

public interface BaseTableProjection<B extends BaseTable> {

    List<Selection<?>> getSelections();

    BaseTableFactory<B, B> getBaseTableFactory();
}
