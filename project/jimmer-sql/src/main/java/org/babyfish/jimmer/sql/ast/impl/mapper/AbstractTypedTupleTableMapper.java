package org.babyfish.jimmer.sql.ast.impl.mapper;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.mapper.TypedTupleTableMapper;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

public abstract class AbstractTypedTupleTableMapper<T, TT extends TableLike<T>>
        extends AbstractTypedTupleMapper<T>
        implements TypedTupleTableMapper<T, TT> {

    private final Class<TT> tupleTableType;

    protected AbstractTypedTupleTableMapper(Class<T> tupleType, Class<TT> tupleTableType, Selection<?>[] selections) {
        super(tupleType, selections);
        this.tupleTableType = tupleTableType;
    }

    @Override
    public Class<TT> getTupleTableType() {
        return tupleTableType;
    }
}
