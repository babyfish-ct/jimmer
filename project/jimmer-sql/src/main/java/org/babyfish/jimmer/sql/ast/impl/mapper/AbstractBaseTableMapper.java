package org.babyfish.jimmer.sql.ast.impl.mapper;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.mapper.BaseTableMapper;
import org.babyfish.jimmer.sql.ast.table.BaseTable;

public abstract class AbstractBaseTableMapper<T, B extends BaseTable<T>>
        extends AbstractTypedTupleMapper<T>
        implements BaseTableMapper<T, B> {

    private final Class<B> baseTableType;

    protected AbstractBaseTableMapper(
            Class<T> tupleType,
            Class<B> baseTableType,
            Selection<?>[] selections
    ) {
        super(tupleType, selections);
        this.baseTableType = baseTableType;
    }

    @Override
    public Class<B> getBaseTableType() {
        return baseTableType;
    }
}
