package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ObjectReader implements Reader<Object> {

    private static final Reader<?>[] EMPTY_READERS = new Reader[0];

    private static final PropId[] EMPTY_PROP_IDS = new PropId[0];

    private final ImmutableType type;

    private final Reader<?> idReader;

    private final PropId[] nonIdPropIds;

    private final Reader<?>[] nonIdReaders;

    private final PropId[] idViewPropIds;

    private final PropId[] idViewBasePropIds;

    @Nullable
    private final List<PropId> shownPropIds;

    @Nullable
    private final List<PropId> hiddenPropsIds;

    ObjectReader(
            ImmutableType type,
            Reader<?> idReader,
            Map<ImmutableProp, Reader<?>> nonIdReaders,
            @Nullable List<PropId> shownPropIds,
            @Nullable List<PropId> hiddenPropsIds
    ) {
        List<PropId> idViewPropIds = new ArrayList<>();
        List<PropId> idViewBasePropIds = new ArrayList<>();
        for (ImmutableProp prop : nonIdReaders.keySet()) {
            ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
            if (idViewBaseProp != null) {
                idViewPropIds.add(prop.getId());
                idViewBasePropIds.add(idViewBaseProp.getId());
            }
        }

        this.type = type;
        this.idReader = idReader;
        this.nonIdPropIds = nonIdReaders.keySet().stream().map(ImmutableProp::getId).toArray(PropId[]::new);
        this.nonIdReaders = nonIdReaders.values().toArray(EMPTY_READERS);
        this.idViewPropIds = idViewPropIds.toArray(EMPTY_PROP_IDS);
        this.idViewBasePropIds = idViewBasePropIds.toArray(EMPTY_PROP_IDS);
        this.shownPropIds = shownPropIds;
        this.hiddenPropsIds = hiddenPropsIds;
    }

    @Override
    public void skip(Context ctx) {
        idReader.skip(ctx);
        for (Reader<?> reader : nonIdReaders) {
            reader.skip(ctx);
        }
    }

    @Override
    public Object read(ResultSet rs, Context ctx) throws SQLException {
        Object id = idReader.read(rs, ctx);
        if (id == null) {
            for (Reader<?> reader : nonIdReaders) {
                reader.skip(ctx);
            }
            return null;
        }
        DraftSpi spi = (DraftSpi) type.getDraftFactory().apply(ctx.draftContext(), null);
        spi.__set(type.getIdProp().getId(), id);
        try {
            int size = nonIdReaders.length;
            for (int i = 0; i < size; i++) {
                Object value = nonIdReaders[i].read(rs, ctx);
                spi.__set(nonIdPropIds[i], value);
            }
            for (int i = idViewBasePropIds.length - 1; i >= 0; i--) {
                spi.__show(idViewPropIds[i], true);
                spi.__show(idViewBasePropIds[i], false);
            }
            if (shownPropIds != null) {
                for (PropId propId : shownPropIds) {
                    spi.__show(propId, true);
                }
            }
            if (hiddenPropsIds != null) {
                for (PropId propId : hiddenPropsIds) {
                    spi.__show(propId, false);
                }
            }
        } catch (Throwable ex) {
            throw DraftConsumerUncheckedException.rethrow(ex);
        }
        return ctx.resolve(spi);
    }
}
