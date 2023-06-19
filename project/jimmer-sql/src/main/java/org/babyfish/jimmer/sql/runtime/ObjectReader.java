package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

class ObjectReader implements Reader<Object> {

    private static final ImmutableProp[] EMPTY_PROPS = new ImmutableProp[0];

    private static final Reader<?>[] EMPTY_READERS = new Reader[0];

    private final ImmutableType type;

    private final Reader<?> idReader;

    private final ImmutableProp[] nonIdProps;

    private final Reader<?>[] nonIdReaders;

    ObjectReader(ImmutableType type, Reader<?> idReader, Map<ImmutableProp, Reader<?>> nonIdReaders) {
        this.type = type;
        this.idReader = idReader;
        this.nonIdProps = nonIdReaders.keySet().toArray(EMPTY_PROPS);
        this.nonIdReaders = nonIdReaders.values().toArray(EMPTY_READERS);
    }

    @Override
    public Object read(ResultSet rs, Context ctx) throws SQLException {
        Object id = idReader.read(rs, ctx);
        if (id == null) {
            ctx.addCol(nonIdReaders.length);
            return null;
        }
        DraftSpi spi = (DraftSpi) type.getDraftFactory().apply(ctx.draftContext(), null);
        spi.__set(type.getIdProp().getId(), id);
        try {
            int size = nonIdReaders.length;
            for (int i = 0; i < size; i++) {
                Object value = nonIdReaders[i].read(rs, ctx);
                spi.__set(nonIdProps[i].getId(), value);
            }
        } catch (Throwable ex) {
            throw DraftConsumerUncheckedException.rethrow(ex);
        }
        return ctx.resolve(spi);
    }
}
