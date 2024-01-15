package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.impl.util.PropCache;
import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.util.EmbeddableObjects;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.SqlTemplate;
import org.babyfish.jimmer.sql.meta.Storage;

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

    private final JSqlClientImplementor sqlClient;

    private TypeCache<Reader<?>> typeReaderCache =
            new TypeCache<>(this::createTypeReader, true);
    
    private PropCache<Reader<?>> propReaderCache =
            new PropCache<>(this::createPropReader, true);

    public ReaderManager(JSqlClientImplementor sqlClient) {
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

        Storage storage = prop.getStorage(sqlClient.getMetadataStrategy());
        if (storage instanceof ColumnDefinition) {
            if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                return new EmbeddedReader(prop.getTargetType(), this);
            }
            if (prop.isReference(TargetLevel.ENTITY)) {
                return new ReferenceReader(prop, this);
            }
            return scalarReader(prop);
        }
        SqlTemplate template = prop.getSqlTemplate();
        if (template instanceof FormulaTemplate) {
            return scalarReader(prop);
        }
        return null;
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
    private Reader<?> scalarReader(ImmutableProp prop) {
        ImmutableType immutableType = prop.getTargetType();
        if (immutableType != null && immutableType.isEmbeddable()) {
            return new EmbeddedReader(immutableType, this);
        }
        Reader<?> reader = prop.isScalarList() ? null : BASE_READER_MAP.get(prop.getElementClass());
        if (reader == null) {
            ScalarProvider<Object, Object> scalarProvider = sqlClient.getScalarProvider(prop);
            if (scalarProvider == null) {
                throw new IllegalArgumentException(
                        "No scalar provider for property \"" +
                                prop +
                                "\""
                );
            }
            Class<?> sqlType = scalarProvider.getSqlType();
            reader = BASE_READER_MAP.get(sqlType);
            if (reader == null) {
                reader = unknownSqlTypeReader(sqlType, scalarProvider, sqlClient.getDialect());
            }
            reader = new CustomizedScalarReader<>(
                    scalarProvider,
                    (Reader<Object>) reader
            );
        }
        return reader;
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
                reader = unknownSqlTypeReader(sqlType, scalarProvider, sqlClient.getDialect());
            }
            reader = new CustomizedScalarReader<>(
                    (ScalarProvider<Object, Object>) scalarProvider,
                    (Reader<Object>) reader
            );
        }
        return reader;
    }

    private static Reader<?> unknownSqlTypeReader(
            Class<?> sqlType,
            ScalarProvider<?, ?> provider,
            Dialect dialect
    ) {
        Reader<?> reader = provider.reader();
        if (reader == null) {
            reader = dialect.unknownReader(sqlType);
            if (reader == null) {
                throw new IllegalStateException(
                        "There is no reader for unknown type \"" +
                                sqlType.getName() +
                                "\" in both \"" +
                                ScalarProvider.class.getName() +
                                "\" and \"" +
                                dialect.getClass().getName() +
                                "\""
                );
            }
        }
        return reader;
    }

    private static class ByteArrayReader implements Reader<byte[]> {

        @Override
        public byte[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getBytes(ctx.col());
        }
    }

    private static class BoxedByteArrayReader implements Reader<Byte[]> {

        @Override
        public Byte[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), Byte[].class);
        }
    }

    private static class ShortArrayReader implements Reader<short[]> {

        @Override
        public short[] read(ResultSet rs, Context ctx) throws SQLException {
            Short[] arr = rs.getObject(ctx.col(), Short[].class);
            if (arr == null) {
                return null;
            }
            short[] primitiveArr = new short[arr.length];
            for (int i = 0; i < arr.length; i++) {
                Short boxed = arr[i];
                primitiveArr[i] = boxed != null ? boxed : 0;
            }
            return primitiveArr;
        }
    }

    private static class BoxedShortArrayReader implements Reader<Short[]> {

        @Override
        public Short[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), Short[].class);
        }
    }

    private static class IntArrayReader implements Reader<int[]> {

        @Override
        public int[] read(ResultSet rs, Context ctx) throws SQLException {
            Integer[] arr = rs.getObject(ctx.col(), Integer[].class);
            if (arr == null) {
                return null;
            }
            int[] primitiveArr = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                Integer boxed = arr[i];
                primitiveArr[i] = boxed != null ? boxed : 0;
            }
            return primitiveArr;
        }
    }

    private static class BoxedIntArrayReader implements Reader<Integer[]> {

        @Override
        public Integer[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), Integer[].class);
        }
    }

    private static class LongArrayReader implements Reader<long[]> {

        @Override
        public long[] read(ResultSet rs, Context ctx) throws SQLException {
            Long[] arr = rs.getObject(ctx.col(), Long[].class);
            if (arr == null) {
                return null;
            }
            long[] primitiveArr = new long[arr.length];
            for (int i = 0; i < arr.length; i++) {
                Long boxed = arr[i];
                primitiveArr[i] = boxed != null ? boxed : 0L;
            }
            return primitiveArr;
        }
    }

    private static class BoxedLongArrayReader implements Reader<Long[]> {

        @Override
        public Long[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), Long[].class);
        }
    }

    private static class FloatArrayReader implements Reader<float[]> {

        @Override
        public float[] read(ResultSet rs, Context ctx) throws SQLException {
            Float[] arr = rs.getObject(ctx.col(), Float[].class);
            if (arr == null) {
                return null;
            }
            float[] primitiveArr = new float[arr.length];
            for (int i = 0; i < arr.length; i++) {
                Float boxed = arr[i];
                primitiveArr[i] = boxed != null ? boxed : 0F;
            }
            return primitiveArr;
        }
    }

    private static class BoxedFloatArrayReader implements Reader<Float[]> {

        @Override
        public Float[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), Float[].class);
        }
    }

    private static class DoubleArrayReader implements Reader<double[]> {

        @Override
        public double[] read(ResultSet rs, Context ctx) throws SQLException {
            Double[] arr = rs.getObject(ctx.col(), Double[].class);
            if (arr == null) {
                return null;
            }
            double[] primitiveArr = new double[arr.length];
            for (int i = 0; i < arr.length; i++) {
                Double boxed = arr[i];
                primitiveArr[i] = boxed != null ? boxed : 0D;
            }
            return primitiveArr;
        }
    }

    private static class BoxedDoubleArrayReader implements Reader<Double[]> {

        @Override
        public Double[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), Double[].class);
        }
    }

    private static class StringArrayReader implements Reader<String[]> {

        @Override
        public String[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), String[].class);
        }
    }

    private static class UUIDArrayReader implements Reader<UUID[]> {

        @Override
        public UUID[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), UUID[].class);
        }
    }

    private static class BooleanReader implements Reader<Boolean> {

        @Override
        public Boolean read(ResultSet rs, Context ctx) throws SQLException {
            boolean value = rs.getBoolean(ctx.col());
            if (!value && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class CharReader implements Reader<Character> {

        @Override
        public Character read(ResultSet rs, Context ctx) throws SQLException {
            String str = rs.getString(ctx.col());
            return str != null ? str.charAt(0) : null;
        }
    }

    private static class ByteReader implements Reader<Byte> {

        @Override
        public Byte read(ResultSet rs, Context ctx) throws SQLException {
            byte value = rs.getByte(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class ShortReader implements Reader<Short> {

        @Override
        public Short read(ResultSet rs, Context ctx) throws SQLException {
            short value = rs.getShort(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class IntReader implements Reader<Integer> {

        @Override
        public Integer read(ResultSet rs, Context ctx) throws SQLException {
            int value = rs.getInt(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class LongReader implements Reader<Long> {

        @Override
        public Long read(ResultSet rs, Context ctx) throws SQLException {
            long value = rs.getLong(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class FloatReader implements Reader<Float> {

        @Override
        public Float read(ResultSet rs, Context ctx) throws SQLException {
            float value = rs.getFloat(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class DoubleReader implements Reader<Double> {

        @Override
        public Double read(ResultSet rs, Context ctx) throws SQLException {
            double value = rs.getDouble(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class BigIntegerReader implements Reader<BigInteger> {

        @Override
        public BigInteger read(ResultSet rs, Context ctx) throws SQLException {
            BigDecimal decimal = rs.getBigDecimal(ctx.col());
            return decimal.toBigInteger();
        }
    }

    private static class BigDecimalReader implements Reader<BigDecimal> {

        @Override
        public BigDecimal read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getBigDecimal(ctx.col());
        }
    }

    private static class StringReader implements Reader<String> {

        @Override
        public String read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getString(ctx.col());
        }
    }

    private static class UUIDReader implements Reader<UUID> {

        @Override
        public UUID read(ResultSet rs, Context ctx) throws SQLException {
            Object obj = rs.getObject(ctx.col());
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
        public Blob read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getBlob(ctx.col());
        }
    }

    private static class SqlDateReader implements Reader<java.sql.Date> {

        @Override
        public java.sql.Date read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getDate(ctx.col());
        }
    }

    private static class SqlTimeReader implements Reader<java.sql.Time> {

        @Override
        public java.sql.Time read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getTime(ctx.col());
        }
    }

    private static class SqlTimestampReader implements Reader<java.sql.Timestamp> {

        @Override
        public Timestamp read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getTimestamp(ctx.col());
        }
    }

    private static class DateReader implements Reader<java.util.Date> {
        @Override
        public java.util.Date read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(ctx.col());
            return timestamp != null ? new java.util.Date(timestamp.getTime()) : null;
        }
    }

    private static class LocalDateReader implements Reader<LocalDate> {

        @Override
        public LocalDate read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(ctx.col());
            return timestamp != null ? timestamp.toLocalDateTime().toLocalDate() : null;
        }
    }

    private static class LocalTimeReader implements Reader<LocalTime> {

        @Override
        public LocalTime read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(ctx.col());
            return timestamp != null ? timestamp.toLocalDateTime().toLocalTime() : null;
        }
    }

    private static class LocalDateTimeReader implements Reader<LocalDateTime> {

        @Override
        public LocalDateTime read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(ctx.col());
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }
    }

    private static class OffsetDateTimeReader implements Reader<OffsetDateTime> {

        @Override
        public OffsetDateTime read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), OffsetDateTime.class);
        }
    }

    private static class ZonedDateTimeReader implements Reader<ZonedDateTime> {

        @Override
        public ZonedDateTime read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getObject(ctx.col(), ZonedDateTime.class);
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
        public T read(ResultSet rs, Context ctx) throws SQLException {
            S sqlValue = sqlReader.read(rs, ctx);
            try {
                return sqlValue != null ? scalarProvider.toScalar(sqlValue) : null;
            } catch (Exception ex) {
                throw new ExecutionException(
                        "Cannot convert \"" +
                                sqlValue +
                                "\" to the jvm type \"" +
                                scalarProvider.getScalarType() +
                                "\"",
                        ex
                );
            }
        }
    }

    private static class ReferenceReader implements Reader<Object> {

        private final ImmutableType targetType;

        private final Reader<?> foreignKeyReader;

        private ReferenceReader(ImmutableProp prop, ReaderManager readerManager) {
            this.targetType = prop.getTargetType();
            this.foreignKeyReader = readerManager.scalarReader(targetType.getIdProp());
        }

        @Override
        public Object read(ResultSet rs, Context ctx) throws SQLException {
            Object fk = foreignKeyReader.read(rs, ctx);
            if (fk == null) {
                return null;
            }
            DraftSpi spi = (DraftSpi) targetType.getDraftFactory().apply(ctx.draftContext(), null);
            try {
                spi.__set(targetType.getIdProp().getId(), fk);
            } catch (Throwable ex) {
                throw DraftConsumerUncheckedException.rethrow(ex);
            }
            return ctx.resolve(spi);
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
        public Association<?, ?> read(ResultSet rs, Context ctx) throws SQLException {
            Object source = sourceReader.read(rs, ctx);
            Object target = targetReader.read(rs, ctx);
            return new Association<>(source, target);
        }
    }

    private static class EmbeddedReader implements Reader<Object> {

        private static final ImmutableProp[] EMPTY_PROPS = new ImmutableProp[0];

        private static final Reader<?>[] EMPTY_READERS = new Reader[0];

        private final ImmutableType targetType;

        private ImmutableProp[] props;

        private Reader<?>[] readers;

        EmbeddedReader(ImmutableType targetType, ReaderManager readerManager) {
            this.targetType = targetType;
            Map<ImmutableProp, Reader<?>> map = new LinkedHashMap<>();
            for (ImmutableProp childProp : targetType.getProps().values()) {
                if (childProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                    map.put(childProp, new EmbeddedReader(childProp.getTargetType(), readerManager));
                } else {
                    map.put(childProp, readerManager.scalarReader(childProp));
                }
            }
            props = map.keySet().toArray(EMPTY_PROPS);
            readers = map.values().toArray(EMPTY_READERS);
        }

        @Override
        public Object read(ResultSet rs, Context ctx) throws SQLException {
            DraftSpi spi = (DraftSpi) targetType.getDraftFactory().apply(ctx.draftContext(), null);
            try {
                int size = readers.length;
                for (int i = 0; i < size; i++) {
                    ImmutableProp prop = props[i];
                    Object value = readers[i].read(rs, ctx);
                    if (value != null || prop.isNullable()) {
                        spi.__set(prop.getId(), value);
                    }
                }
            } catch (Throwable ex) {
                return DraftConsumerUncheckedException.rethrow(ex);
            }
            Object embeddable = ctx.resolve(spi);
            return EmbeddableObjects.isCompleted(embeddable) ? embeddable : null;
        }
    }

    public static boolean isStandardScalarType(Class<?> type) {
        return BASE_READER_MAP.containsKey(type);
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
        map.put(short[].class, new ShortArrayReader());
        map.put(Short[].class, new BoxedShortArrayReader());
        map.put(int.class, new IntReader());
        map.put(Integer.class, new IntReader());
        map.put(int[].class, new IntArrayReader());
        map.put(Integer[].class, new BoxedIntArrayReader());
        map.put(long.class, new LongReader());
        map.put(Long.class, new LongReader());
        map.put(long[].class, new LongArrayReader());
        map.put(Long[].class, new BoxedLongArrayReader());
        map.put(float.class, new FloatReader());
        map.put(Float.class, new FloatReader());
        map.put(float[].class, new FloatArrayReader());
        map.put(Float[].class, new BoxedFloatArrayReader());
        map.put(double.class, new DoubleReader());
        map.put(Double.class, new DoubleReader());
        map.put(double[].class, new DoubleArrayReader());
        map.put(Double[].class, new BoxedDoubleArrayReader());
        map.put(BigInteger.class, new BigIntegerReader());
        map.put(BigDecimal.class, new BigDecimalReader());
        map.put(String.class, new StringReader());
        map.put(String[].class, new StringArrayReader());
        map.put(UUID.class, new UUIDReader());
        map.put(UUID[].class, new UUIDArrayReader());
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
