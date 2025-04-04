package org.babyfish.jimmer.sql.runtime;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class DynamicEmbeddedReader implements Reader<Object> {

    private final ImmutableType type;

    private final List<ImmutableProp> props;

    private final List<Reader<?>> readers;

    @Nullable
    private final List<PropId> shownPropIds;

    @Nullable
    private final List<PropId> hiddenPropIds;

    DynamicEmbeddedReader(
            ImmutableType type,
            List<ImmutableProp> props,
            List<Reader<?>> readers,
            @Nullable List<PropId> shownPropIds,
            @Nullable List<PropId> hiddenPropIds
    ) {
        this.type = type;
        this.props = props;
        this.readers = readers;
        this.shownPropIds = shownPropIds;
        this.hiddenPropIds = hiddenPropIds;
    }

    @Override
    public Object read(ResultSet rs, Context ctx) throws SQLException {
        DraftSpi spi = (DraftSpi) type.getDraftFactory().apply(ctx.draftContext(), null);
        boolean hasNoNull = false;
        boolean hasRequiredNull = false;
        try {
            int size = readers.size();
            for (int i = 0; i < size; i++) {
                Object value = readers.get(i).read(rs, ctx);
                if (hasRequiredNull) {
                    continue;
                }
                ImmutableProp prop = props.get(i);
                if (value == null) {
                    if (prop.isNullable()) {
                        spi.__set(prop.getId(), null);
                    } else {
                        hasRequiredNull = true;
                    }
                } else {
                    spi.__set(prop.getId(), value);
                    hasNoNull = true;
                }
            }
        } catch (Throwable ex) {
            throw DraftConsumerUncheckedException.rethrow(ex);
        }
        if (shownPropIds != null) {
            for (PropId propId : shownPropIds) {
                spi.__show(propId, true);
            }
        }
        if (hiddenPropIds != null) {
            for (PropId propId : hiddenPropIds) {
                spi.__show(propId, false);
            }
        }
        return hasNoNull && !hasRequiredNull ? ctx.resolve(spi) : null;
    }

    @Override
    public String toString() {
        return "DynamicEmbeddedReader{" +
                "type=" + type +
                ", propIds=" + props +
                ", rReaders=" + readers +
                '}';
    }
}
