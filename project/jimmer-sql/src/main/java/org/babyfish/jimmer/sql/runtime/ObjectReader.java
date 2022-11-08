package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

class ObjectReader implements Reader<Object> {

    private final ImmutableType type;

    private final Reader<?> idReader;

    private final Map<ImmutableProp, Reader<?>> nonIdReaders;

    ObjectReader(ImmutableType type, Reader<?> idReader, Map<ImmutableProp, Reader<?>> nonIdReaders) {
        this.type = type;
        this.idReader = idReader;
        this.nonIdReaders = nonIdReaders;
    }

    @Override
    public Object read(ResultSet rs, Col col) throws SQLException {
        Object id = idReader.read(rs, col);
        if (id == null) {
            col.add(nonIdReaders.size());
            return null;
        }
        return Internal.produce(type, null, draft -> {
            DraftSpi spi = (DraftSpi) draft;
            spi.__set(type.getIdProp().getId(), id);
            for (Map.Entry<ImmutableProp, Reader<?>> e : nonIdReaders.entrySet()) {
                ImmutableProp prop = e.getKey();
                Reader<?> reader = e.getValue();
                Object value = reader.read(rs, col);
                spi.__set(prop.getId(), value);
            }
        });
    }
}
