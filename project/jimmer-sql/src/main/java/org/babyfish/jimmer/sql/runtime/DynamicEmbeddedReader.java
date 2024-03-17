package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class DynamicEmbeddedReader implements Reader<Object> {

    private final ImmutableType type;

    private final List<ImmutableProp> props;

    private final List<Reader<?>> readers;

    DynamicEmbeddedReader(ImmutableType type, List<ImmutableProp> props, List<Reader<?>> readers) {
        this.type = type;
        this.props = props;
        this.readers = readers;
    }

    @Override
    public Object read(ResultSet rs, Context ctx) throws SQLException {
        DraftSpi spi = (DraftSpi) type.getDraftFactory().apply(ctx.draftContext(), null);
        try {
            int size = readers.size();
            for (int i = 0; i < size; i++) {
                Object value = readers.get(i).read(rs, ctx);
                ImmutableProp prop = props.get(i);
                if (value == null && !prop.isNullable()) {
                    return null;
                }
                spi.__set(prop.getId(), value);
            }
        } catch (Throwable ex) {
            throw DraftConsumerUncheckedException.rethrow(ex);
        }
        return ctx.resolve(spi);
    }

    @Override
    public String toString() {
        return "EmbeddableReader{" +
                "type=" + type +
                ", propIds=" + props +
                ", rReaders=" + readers +
                '}';
    }
}
