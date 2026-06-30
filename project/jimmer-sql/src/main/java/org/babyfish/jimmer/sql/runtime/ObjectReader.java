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

    @Nullable
    private final Reader<?> discriminatorReader;

    @Nullable
    private final PropId discriminatorPropId;

    @Nullable
    private final Map<Object, ImmutableType> discriminatorTypeMap;

    private final TypeBranchReader[] typeBranchReaders;

    @Nullable
    private final List<PropId> shownPropIds;

    @Nullable
    private final List<PropId> hiddenPropsIds;

    ObjectReader(
            ImmutableType type,
            Reader<?> idReader,
            Map<ImmutableProp, Reader<?>> nonIdReaders,
            @Nullable Reader<?> discriminatorReader,
            @Nullable List<TypeBranchReader> typeBranchReaders,
            @Nullable List<PropId> shownPropIds,
            @Nullable List<PropId> hiddenPropsIds
    ) {
        Reader<?> actualDiscriminatorReader = discriminatorReader(type, discriminatorReader);
        List<PropId> idViewPropIds = new ArrayList<>();
        List<PropId> idViewBasePropIds = new ArrayList<>();
        List<PropId> nonIdPropIds = new ArrayList<>();
        List<Reader<?>> nonIdReaderList = new ArrayList<>();
        PropId discriminatorPropId = null;
        for (Map.Entry<ImmutableProp, Reader<?>> e : nonIdReaders.entrySet()) {
            ImmutableProp prop = e.getKey();
            if (actualDiscriminatorReader != null && prop.isDiscriminator()) {
                discriminatorPropId = prop.getId();
                continue;
            }
            ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
            if (idViewBaseProp != null) {
                idViewPropIds.add(prop.getId());
                idViewBasePropIds.add(idViewBaseProp.getId());
            }
            nonIdPropIds.add(prop.getId());
            nonIdReaderList.add(e.getValue());
        }

        this.type = type;
        this.idReader = idReader;
        this.nonIdPropIds = nonIdPropIds.toArray(EMPTY_PROP_IDS);
        this.nonIdReaders = nonIdReaderList.toArray(EMPTY_READERS);
        this.idViewPropIds = idViewPropIds.toArray(EMPTY_PROP_IDS);
        this.idViewBasePropIds = idViewBasePropIds.toArray(EMPTY_PROP_IDS);
        this.discriminatorReader = actualDiscriminatorReader;
        this.discriminatorPropId = discriminatorPropId;
        this.discriminatorTypeMap = actualDiscriminatorReader != null ?
                type.getInheritanceInfo().getDiscriminatorTypeMap() :
                null;
        this.typeBranchReaders = typeBranchReaders != null ?
                typeBranchReaders.toArray(new TypeBranchReader[0]) :
                new TypeBranchReader[0];
        this.shownPropIds = shownPropIds;
        this.hiddenPropsIds = hiddenPropsIds;
    }

    @Nullable
    static Reader<?> discriminatorReader(JSqlClientImplementor sqlClient, ImmutableType type) {
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null) {
            return null;
        }
        if (!requiresDiscriminator(inheritanceInfo, type)) {
            return null;
        }
        return sqlClient.getReader(inheritanceInfo.getDiscriminatorProp());
    }

    @Nullable
    private static Reader<?> discriminatorReader(ImmutableType type, @Nullable Reader<?> reader) {
        if (reader == null) {
            return null;
        }
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null ||
                !requiresDiscriminator(inheritanceInfo, type)) {
            return null;
        }
        return reader;
    }

    private static boolean requiresDiscriminator(InheritanceInfo inheritanceInfo, ImmutableType type) {
        java.util.Collection<ImmutableType> concreteTypes = inheritanceInfo.getConcreteTypes(type);
        return concreteTypes.size() != 1 || concreteTypes.iterator().next() != type;
    }

    @Override
    public void skip(Context ctx) {
        idReader.skip(ctx);
        if (discriminatorReader != null) {
            discriminatorReader.skip(ctx);
        }
        for (Reader<?> reader : nonIdReaders) {
            reader.skip(ctx);
        }
        for (TypeBranchReader typeBranchReader : typeBranchReaders) {
            typeBranchReader.skip(ctx);
        }
    }

    @Override
    public Object read(ResultSet rs, Context ctx) throws SQLException {
        Object id = idReader.read(rs, ctx);
        if (id == null) {
            if (discriminatorReader != null) {
                discriminatorReader.skip(ctx);
            }
            for (Reader<?> reader : nonIdReaders) {
                reader.skip(ctx);
            }
            for (TypeBranchReader typeBranchReader : typeBranchReaders) {
                typeBranchReader.skip(ctx);
            }
            return null;
        }
        Object discriminatorValue = readDiscriminator(rs, ctx);
        ImmutableType actualType = actualType(discriminatorValue);
        DraftSpi spi = (DraftSpi) actualType.getDraftFactory().apply(ctx.draftContext(), null);
        spi.__set(type.getIdProp().getId(), id);
        setDiscriminator(spi, discriminatorValue);
        int size = nonIdReaders.length;
        for (int i = 0; i < size; i++) {
            set(spi, nonIdPropIds[i], nonIdReaders[i].read(rs, ctx));
        }
        showOrHide(spi);
        for (TypeBranchReader typeBranchReader : typeBranchReaders) {
            typeBranchReader.read(rs, ctx, actualType, spi);
        }
        return ctx.resolve(spi);
    }

    @Nullable
    private Object readDiscriminator(ResultSet rs, Context ctx) throws SQLException {
        Reader<?> reader = discriminatorReader;
        return reader != null ? reader.read(rs, ctx) : null;
    }

    private ImmutableType actualType(@Nullable Object discriminatorValue) {
        if (discriminatorReader == null) {
            return type;
        }
        Map<Object, ImmutableType> map = discriminatorTypeMap;
        if (map == null) {
            return type;
        }
        ImmutableType actualType = map.get(discriminatorValue);
        if (actualType == null) {
            throw new ExecutionException(
                    "Cannot resolve the concrete type of \"" +
                            type +
                            "\" because there is no type mapped by discriminator value \"" +
                            discriminatorValue +
                            "\""
            );
        }
        return actualType;
    }

    private void setDiscriminator(DraftSpi spi, @Nullable Object discriminatorValue) {
        PropId propId = discriminatorPropId;
        if (propId != null) {
            set(spi, propId, discriminatorValue);
        }
    }

    private void showOrHide(DraftSpi spi) {
        try {
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
    }

    private static void set(DraftSpi spi, PropId propId, Object value) {
        try {
            spi.__set(propId, value);
        } catch (Throwable ex) {
            throw DraftConsumerUncheckedException.rethrow(ex);
        }
    }

    static class TypeBranchReader {

        private final ImmutableType type;

        private final PropId[] nonIdPropIds;

        private final Reader<?>[] nonIdReaders;

        private final PropId[] idViewPropIds;

        private final PropId[] idViewBasePropIds;

        @Nullable
        private final List<PropId> shownPropIds;

        @Nullable
        private final List<PropId> hiddenPropsIds;

        TypeBranchReader(
                ImmutableType type,
                Map<ImmutableProp, Reader<?>> nonIdReaders,
                @Nullable List<PropId> shownPropIds,
                @Nullable List<PropId> hiddenPropsIds
        ) {
            List<PropId> idViewPropIds = new ArrayList<>();
            List<PropId> idViewBasePropIds = new ArrayList<>();
            List<PropId> nonIdPropIds = new ArrayList<>();
            List<Reader<?>> nonIdReaderList = new ArrayList<>();
            for (Map.Entry<ImmutableProp, Reader<?>> e : nonIdReaders.entrySet()) {
                ImmutableProp prop = e.getKey();
                if (prop.isDiscriminator()) {
                    continue;
                }
                ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
                if (idViewBaseProp != null) {
                    idViewPropIds.add(prop.getId());
                    idViewBasePropIds.add(idViewBaseProp.getId());
                }
                nonIdPropIds.add(prop.getId());
                nonIdReaderList.add(e.getValue());
            }
            this.type = type;
            this.nonIdPropIds = nonIdPropIds.toArray(EMPTY_PROP_IDS);
            this.nonIdReaders = nonIdReaderList.toArray(EMPTY_READERS);
            this.idViewPropIds = idViewPropIds.toArray(EMPTY_PROP_IDS);
            this.idViewBasePropIds = idViewBasePropIds.toArray(EMPTY_PROP_IDS);
            this.shownPropIds = shownPropIds;
            this.hiddenPropsIds = hiddenPropsIds;
        }

        void skip(Context ctx) {
            for (Reader<?> reader : nonIdReaders) {
                reader.skip(ctx);
            }
        }

        void read(ResultSet rs, Context ctx, ImmutableType actualType, DraftSpi spi) throws SQLException {
            if (!type.isAssignableFrom(actualType)) {
                skip(ctx);
                return;
            }
            for (int i = 0; i < nonIdReaders.length; i++) {
                set(spi, nonIdPropIds[i], nonIdReaders[i].read(rs, ctx));
            }
            showOrHide(spi);
        }

        private void showOrHide(DraftSpi spi) {
            try {
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
        }
    }
}
