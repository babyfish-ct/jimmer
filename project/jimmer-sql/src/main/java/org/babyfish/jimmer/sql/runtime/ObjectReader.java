package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.exception.ExecutionException;
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

    private final int discriminatorPropIndex;

    @Nullable
    private final Reader<?> discriminatorReader;

    @Nullable
    private final Map<String, ImmutableType> discriminatorTypeMap;

    @Nullable
    private final List<PropId> shownPropIds;

    @Nullable
    private final List<PropId> hiddenPropsIds;

    ObjectReader(
            ImmutableType type,
            Reader<?> idReader,
            Map<ImmutableProp, Reader<?>> nonIdReaders,
            @Nullable Reader<?> discriminatorReader,
            @Nullable List<PropId> shownPropIds,
            @Nullable List<PropId> hiddenPropsIds
    ) {
        List<PropId> idViewPropIds = new ArrayList<>();
        List<PropId> idViewBasePropIds = new ArrayList<>();
        int discriminatorPropIndex = -1;
        int index = 0;
        for (ImmutableProp prop : nonIdReaders.keySet()) {
            ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
            if (idViewBaseProp != null) {
                idViewPropIds.add(prop.getId());
                idViewBasePropIds.add(idViewBaseProp.getId());
            }
            if (prop.isDiscriminator()) {
                discriminatorPropIndex = index;
            }
            index++;
        }

        this.type = type;
        this.idReader = idReader;
        this.nonIdPropIds = nonIdReaders.keySet().stream().map(ImmutableProp::getId).toArray(PropId[]::new);
        this.nonIdReaders = nonIdReaders.values().toArray(EMPTY_READERS);
        this.idViewPropIds = idViewPropIds.toArray(EMPTY_PROP_IDS);
        this.idViewBasePropIds = idViewBasePropIds.toArray(EMPTY_PROP_IDS);
        this.discriminatorPropIndex = discriminatorPropIndex;
        this.discriminatorReader = discriminatorPropIndex == -1 ? discriminatorReader : null;
        this.discriminatorTypeMap = discriminatorPropIndex != -1 || discriminatorReader != null ?
                type.getInheritanceInfo().getDiscriminatorTypeMap() :
                null;
        this.shownPropIds = shownPropIds;
        this.hiddenPropsIds = hiddenPropsIds;
    }

    @Nullable
    static Reader<?> discriminatorReader(JSqlClientImplementor sqlClient, ImmutableType type) {
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null ||
                inheritanceInfo.getRootType() != type ||
                inheritanceInfo.getDiscriminatorColumn() == null) {
            return null;
        }
        return sqlClient.getReader(String.class);
    }

    @Override
    public void skip(Context ctx) {
        idReader.skip(ctx);
        for (Reader<?> reader : nonIdReaders) {
            reader.skip(ctx);
        }
        if (discriminatorReader != null) {
            discriminatorReader.skip(ctx);
        }
    }

    @Override
    public Object read(ResultSet rs, Context ctx) throws SQLException {
        Object id = idReader.read(rs, ctx);
        if (id == null) {
            for (Reader<?> reader : nonIdReaders) {
                reader.skip(ctx);
            }
            if (discriminatorReader != null) {
                discriminatorReader.skip(ctx);
            }
            return null;
        }
        Object[] values = new Object[nonIdReaders.length];
        int size = nonIdReaders.length;
        for (int i = 0; i < size; i++) {
            values[i] = nonIdReaders[i].read(rs, ctx);
        }
        ImmutableType actualType = readActualType(rs, ctx, values);
        DraftSpi spi = (DraftSpi) actualType.getDraftFactory().apply(ctx.draftContext(), null);
        spi.__set(type.getIdProp().getId(), id);
        try {
            for (int i = 0; i < size; i++) {
                spi.__set(nonIdPropIds[i], values[i]);
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

    private ImmutableType readActualType(ResultSet rs, Context ctx, Object[] values) throws SQLException {
        Object value;
        if (discriminatorPropIndex != -1) {
            value = values[discriminatorPropIndex];
        } else {
            Reader<?> reader = discriminatorReader;
            if (reader == null) {
                return type;
            }
            value = reader.read(rs, ctx);
        }
        Map<String, ImmutableType> map = discriminatorTypeMap;
        if (map == null) {
            return type;
        }
        if (!(value instanceof String)) {
            throw new ExecutionException(
                    "Cannot resolve the concrete type of \"" +
                            type +
                            "\" because the discriminator value is " +
                            (value != null ? "\"" + value + "\"" : "null")
            );
        }
        ImmutableType actualType = map.get(value);
        if (actualType == null) {
            throw new ExecutionException(
                    "Cannot resolve the concrete type of \"" +
                            type +
                            "\" because there is no subtype mapped by discriminator value \"" +
                            value +
                            "\""
            );
        }
        return actualType;
    }
}
