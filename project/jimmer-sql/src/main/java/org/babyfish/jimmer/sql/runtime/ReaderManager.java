package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.util.EmbeddableObjects;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.impl.util.StaticCache;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

public class ReaderManager {

    private static final Map<Class<?>, Reader<?>> BASE_READER_MAP;

    private final JSqlClient sqlClient;

    private StaticCache<ImmutableType, Reader<?>> typeReaderCache =
            new StaticCache<>(this::createTypeReader, true);
    
    private StaticCache<ImmutableProp, Reader<?>> propReaderCache =
            new StaticCache<>(this::createPropReader, true);

    public ReaderManager(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }
    
    public Reader<?> reader(Class<?> type) {
        ImmutableType immutableType = ImmutableType.tryGet(type);
        return immutableType != null ? reader(immutableType) : scalarReader(type);
    }

    public Reader<?> reader(ImmutableType type) {
        return typeReaderCache.get(type);
    }
    
    public Reader<?> reader(ImmutableProp prop) {
        return propReaderCache.get(prop);
    }

    @SuppressWarnings("unchecked")
    private Reader<?> createPropReader(ImmutableProp prop) {
        Storage storage = prop.getStorage();
        if (!(storage instanceof ColumnDefinition)) {
            return null;
        }
        if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
            return new EmbeddedReader(prop.getTargetType(), this);
        }
        if (prop.isReference(TargetLevel.ENTITY)) {
            return new ReferenceReader(prop, this);
        }
        return scalarReader(prop.getElementClass());
    }

    private Reader<?> createTypeReader(ImmutableType immutableType) {
        if (immutableType.isEmbeddable()) {
            return new EmbeddedReader(immutableType, this);
        }
        if (immutableType instanceof AssociationType) {
            return new AssociationReader((AssociationType) immutableType, this);
        }
        if (!immutableType.isEntity()) {
            return null;
        }
        Map<ImmutableProp, Reader<?>> nonIdReaderMap = new LinkedHashMap<>();
        Reader<?> idReader = null;
        for (ImmutableProp prop : immutableType.getSelectableProps().values()) {
            if (prop.isId()) {
                idReader = reader(prop);
            } else {
                nonIdReaderMap.put(prop, reader(prop));
            }
        }
        return new ObjectReader(immutableType, idReader, nonIdReaderMap);
    }

    @SuppressWarnings("unchecked")
    private Reader<?> scalarReader(Class<?> type) {
        ImmutableType immutableType = ImmutableType.tryGet(type);
        if (immutableType != null && immutableType.isEmbeddable()) {
            return new EmbeddedReader(immutableType, this);
        }
        Reader<?> reader = BASE_READER_MAP.get(type);
        if (reader == null) {
            ScalarProvider<?, ?> scalarProvider = sqlClient.getScalarProvider(type);
            if (scalarProvider == null) {
                throw new IllegalArgumentException(
                        "No scalar provider for customized scalar type \"" +
                                type.getName() +
                                "\""
                );
            }
            Class<?> sqlType = scalarProvider.getSqlType();
            reader = BASE_READER_MAP.get(sqlType);
            if (reader == null) {
                throw new ModelException(
                        "The scalar provider type \"" +
                                scalarProvider.getClass().getName() +
                                "\" is illegal, its sql type \"" +
                                sqlType.getName() +
                                "\" must be one of " +
                                BASE_READER_MAP.keySet()
                );
            }
            reader = new CustomizedScalarReader<>(
                    (ScalarProvider<Object, Object>) scalarProvider,
                    (Reader<Object>) reader
            );
        }
        return reader;
    }

    private static class BooleanReader implements Reader<Boolean> {

        @Override
        public Boolean read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), Boolean.class);
        }
    }

    private static class CharReader implements Reader<Character> {

        @Override
        public Character read(ResultSet rs, Col col) throws SQLException {
            String str = rs.getString(col.get());
            return str != null ? str.charAt(0) : null;
        }
    }

    private static class ByteReader implements Reader<Byte> {

        @Override
        public Byte read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), Byte.class);
        }
    }

    private static class ByteArrayReader implements Reader<byte[]> {

        @Override
        public byte[] read(ResultSet rs, Col col) throws SQLException {
            return rs.getBytes(col.get());
        }
    }

    private static class BoxedByteArrayReader implements Reader<Byte[]> {

        @Override
        public Byte[] read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), Byte[].class);
        }
    }

    private static class ShortReader implements Reader<Short> {

        @Override
        public Short read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), Short.class);
        }
    }

    private static class IntReader implements Reader<Integer> {

        @Override
        public Integer read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), Integer.class);
        }
    }

    private static class LongReader implements Reader<Long> {

        @Override
        public Long read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), Long.class);
        }
    }

    private static class FloatReader implements Reader<Float> {

        @Override
        public Float read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), Float.class);
        }
    }

    private static class DoubleReader implements Reader<Double> {

        @Override
        public Double read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), Double.class);
        }
    }

    private static class BigIntegerReader implements Reader<BigInteger> {

        @Override
        public BigInteger read(ResultSet rs, Col col) throws SQLException {
            BigDecimal decimal = rs.getBigDecimal(col.get());
            return decimal.toBigInteger();
        }
    }

    private static class BigDecimalReader implements Reader<BigDecimal> {

        @Override
        public BigDecimal read(ResultSet rs, Col col) throws SQLException {
            return rs.getBigDecimal(col.get());
        }
    }

    private static class StringReader implements Reader<String> {

        @Override
        public String read(ResultSet rs, Col col) throws SQLException {
            return rs.getString(col.get());
        }
    }

    private static class UUIDReader implements Reader<UUID> {

        @Override
        public UUID read(ResultSet rs, Col col) throws SQLException {
            Object obj = rs.getObject(col.get());
            if (obj == null) {
                return null;
            }
            if (obj instanceof byte[]) {
                ByteBuffer byteBuffer = ByteBuffer.wrap((byte[]) obj);
                long high = byteBuffer.getLong();
                long low = byteBuffer.getLong();
                return new UUID(high, low);
            }
            return UUID.fromString(obj.toString());
        }
    }

    private static class BlobReader implements Reader<Blob> {

        @Override
        public Blob read(ResultSet rs, Col col) throws SQLException {
            return rs.getBlob(col.get());
        }
    }

    private static class SqlDateReader implements Reader<java.sql.Date> {

        @Override
        public java.sql.Date read(ResultSet rs, Col col) throws SQLException {
            return rs.getDate(col.get());
        }
    }

    private static class SqlTimeReader implements Reader<java.sql.Time> {

        @Override
        public java.sql.Time read(ResultSet rs, Col col) throws SQLException {
            return rs.getTime(col.get());
        }
    }

    private static class SqlTimestampReader implements Reader<java.sql.Timestamp> {

        @Override
        public Timestamp read(ResultSet rs, Col col) throws SQLException {
            return rs.getTimestamp(col.get());
        }
    }

    private static class DateReader implements Reader<java.util.Date> {
        @Override
        public java.util.Date read(ResultSet rs, Col col) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(col.get());
            return timestamp != null ? new java.util.Date(timestamp.getTime()) : null;
        }
    }

    private static class LocalDateReader implements Reader<LocalDate> {

        @Override
        public LocalDate read(ResultSet rs, Col col) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(col.get());
            return timestamp != null ? timestamp.toLocalDateTime().toLocalDate() : null;
        }
    }

    private static class LocalTimeReader implements Reader<LocalTime> {

        @Override
        public LocalTime read(ResultSet rs, Col col) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(col.get());
            return timestamp != null ? timestamp.toLocalDateTime().toLocalTime() : null;
        }
    }

    private static class LocalDateTimeReader implements Reader<LocalDateTime> {

        @Override
        public LocalDateTime read(ResultSet rs, Col col) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(col.get());
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }
    }

    private static class OffsetDateTimeReader implements Reader<OffsetDateTime> {

        @Override
        public OffsetDateTime read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), OffsetDateTime.class);
        }
    }

    private static class ZonedDateTimeReader implements Reader<ZonedDateTime> {

        @Override
        public ZonedDateTime read(ResultSet rs, Col col) throws SQLException {
            return rs.getObject(col.get(), ZonedDateTime.class);
        }
    }

    private static class CustomizedScalarReader<T, S> implements Reader<T> {

        private final ScalarProvider<T, S> scalarProvider;

        private final Reader<S> sqlReader;

        CustomizedScalarReader(ScalarProvider<T, S> scalarProvider, Reader<S> sqlReader) {
            this.scalarProvider = scalarProvider;
            this.sqlReader = sqlReader;
        }

        @Override
        public T read(ResultSet rs, Col col) throws SQLException {
            S sqlValue = sqlReader.read(rs, col);
            return sqlValue != null ? scalarProvider.toScalar(sqlValue) : null;
        }
    }

    private static class ReferenceReader implements Reader<Object> {

        private final ImmutableType targetType;

        private final Reader<?> foreignKeyReader;

        private ReferenceReader(ImmutableProp prop, ReaderManager readerManager) {
            this.targetType = prop.getTargetType();
            this.foreignKeyReader = readerManager.scalarReader(targetType.getIdProp().getElementClass());
        }

        @Override
        public Object read(ResultSet rs, Col col) throws SQLException {
            Object fk = foreignKeyReader.read(rs, col);
            if (fk == null) {
                return null;
            }
            return Internal.produce(targetType, null, draft -> {
                ((DraftSpi) draft).__set(targetType.getIdProp().getId(), fk);
            });
        }
    }

    private static class AssociationReader implements Reader<Association<?, ?>> {

        private final ImmutableType sourceType;

        private final ImmutableType targetType;

        private final ImmutableProp sourceIdProp;

        private final ImmutableProp targetIdProp;
        
        private final Reader<?> sourceReader;

        private final Reader<?> targetReader;

        AssociationReader(AssociationType associationType, ReaderManager readerManager) {
            sourceType = associationType.getSourceType();
            targetType = associationType.getTargetType();
            sourceIdProp = sourceType.getIdProp();
            targetIdProp = targetType.getIdProp();
            sourceReader = new ReferenceReader(associationType.getSourceProp(), readerManager);
            targetReader = new ReferenceReader(associationType.getTargetProp(), readerManager);
        }

        @Override
        public Association<?, ?> read(ResultSet rs, Col col) throws SQLException {
            Object source = sourceReader.read(rs, col);
            Object target = targetReader.read(rs, col);
            return new Association<>(source, target);
        }
    }

    private static class EmbeddedReader implements Reader<Object> {

        private final ImmutableType targetType;

        private Map<ImmutableProp, Reader<?>> readerMap;

        EmbeddedReader(ImmutableType targetType, ReaderManager readerManager) {
            this.targetType = targetType;
            Map<ImmutableProp, Reader<?>> map = new LinkedHashMap<>();
            for (ImmutableProp childProp : targetType.getProps().values()) {
                if (childProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                    map.put(childProp, new EmbeddedReader(childProp.getTargetType(), readerManager));
                } else {
                    map.put(childProp, readerManager.scalarReader(childProp.getElementClass()));
                }
            }
            this.readerMap = map;
        }

        @Override
        public Object read(ResultSet rs, Col col) throws SQLException {
            Object embeddable = Internal.produce(targetType, null, draft -> {
                DraftSpi spi = (DraftSpi) draft;
                for (Map.Entry<ImmutableProp, Reader<?>> e : readerMap.entrySet()) {
                    ImmutableProp prop = e.getKey();
                    Object value = e.getValue().read(rs, col);
                    if (value != null || prop.isNullable()) {
                        spi.__set(prop.getId(), value);
                    }
                }
            });
            return EmbeddableObjects.isCompleted(embeddable) ? embeddable : null;
        }
    }

    static {
        Map<Class<?>, Reader<?>> map = new HashMap<>();
        map.put(boolean.class, new BooleanReader());
        map.put(Boolean.class, new BooleanReader());
        map.put(char.class, new CharReader());
        map.put(Character.class, new CharReader());
        map.put(byte.class, new ByteReader());
        map.put(Byte.class, new ByteReader());
        map.put(byte[].class, new ByteArrayReader());
        map.put(Byte[].class, new BoxedByteArrayReader());
        map.put(short.class, new ShortReader());
        map.put(Short.class, new ShortReader());
        map.put(int.class, new IntReader());
        map.put(Integer.class, new IntReader());
        map.put(long.class, new LongReader());
        map.put(Long.class, new LongReader());
        map.put(float.class, new FloatReader());
        map.put(Float.class, new FloatReader());
        map.put(double.class, new DoubleReader());
        map.put(Double.class, new DoubleReader());
        map.put(BigInteger.class, new BigIntegerReader());
        map.put(BigDecimal.class, new BigDecimalReader());
        map.put(String.class, new StringReader());
        map.put(UUID.class, new UUIDReader());
        map.put(Blob.class, new BlobReader());
        map.put(java.sql.Date.class, new SqlDateReader());
        map.put(java.sql.Time.class, new SqlTimeReader());
        map.put(java.sql.Timestamp.class, new SqlTimestampReader());
        map.put(java.util.Date.class, new DateReader());
        map.put(LocalDate.class, new LocalDateReader());
        map.put(LocalTime.class, new LocalTimeReader());
        map.put(LocalDateTime.class, new LocalDateTimeReader());
        map.put(OffsetDateTime.class, new OffsetDateTimeReader());
        map.put(ZonedDateTime.class, new ZonedDateTimeReader());
        BASE_READER_MAP = map;
    }
}
