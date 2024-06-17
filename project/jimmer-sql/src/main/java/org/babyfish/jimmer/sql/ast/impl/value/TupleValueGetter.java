package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

class TupleValueGetter implements ValueGetter {

    private final int index;

    private final ValueGetter next;

    TupleValueGetter(int index, ValueGetter next) {
        this.index = index;
        this.next = next;
    }

    @Override
    public String columnName() {
        return next.columnName();
    }

    @Override
    public Object get(Object value) {
        return next.get(((Tuple2<?, ?>) value).get(index));
    }

    @Override
    public GetterMetadata metadata() {
        return next.metadata();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(index) * 31 + next.hashCode();
    }

    @Override
    public String toString() {
        return "Tuple[" + index + "]." + next;
    }
}
