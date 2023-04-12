package org.babyfish.jimmer.sql.fetcher.spi;

import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.FieldFilterArgs;

public interface FieldFilterArgsImplementor<T extends Table<?>> extends FieldFilterArgs<T> {

    AbstractMutableQueryImpl query();
}