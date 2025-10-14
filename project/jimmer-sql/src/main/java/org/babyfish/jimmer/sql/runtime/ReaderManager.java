package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.DraftConsumerUncheckedException;
import org.babyfish.jimmer.impl.util.CollectionUtils;
import org.babyfish.jimmer.impl.util.PropCache;
import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.Serialized;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.babyfish.jimmer.sql.meta.SqlTemplate;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

    private static final Map<Class<?>, Reader<?>> SIMPLE_LIST_READER_MAP;

    private final JSqlClientImplementor sqlClient;

    private final TypeCache<Reader<?>> typeReaderCache =
            new TypeCache<>(this::createTypeReader, true);

    private final PropCache<Reader<?>> propReaderCache =
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

    private Reader<?> createPropReader(ImmutableProp prop) {

        if (prop.isColumnDefinition()) {
            if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                return new FixedEmbeddedReader(prop.getTargetType(), this);
            }
            if (prop.isReference(TargetLevel.ENTITY)) {
                return new ReferenceReader(prop, this);
            }
            return scalarReader(prop);
        } else if (prop.getDeclaringType().isEmbeddable()) {
            return scalarReader(prop);
        }

        ImmutableProp idViewBaseProp = prop.getIdViewBaseProp();
        if (idViewBaseProp != null && idViewBaseProp.isColumnDefinition()) {
            return new ReferenceIdViewReader(prop, this);
        }

        SqlTemplate template = prop.getSqlTemplate();
        if (template instanceof FormulaTemplate) {
            return scalarReader(prop);
        }
        return null;
    }

    private Reader<?> createTypeReader(ImmutableType immutableType) {
        if (immutableType.isEmbeddable()) {
            return new FixedEmbeddedReader(immutableType, this);
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
                ImmutableProp idViewProp = prop.getIdViewProp();
                if (idViewProp != null) {
                    nonIdReaderMap.put(idViewProp, reader(idViewProp));
                } else {
                    nonIdReaderMap.put(prop, reader(prop));
                }
            }
        }
        return new ObjectReader(immutableType, idReader, nonIdReaderMap, null, null);
    }

    private Reader<?> scalarReader(ImmutableProp prop) {
        ImmutableType immutableType = prop.getTargetType();
        if (immutableType != null && immutableType.isEmbeddable()) {
            return new FixedEmbeddedReader(immutableType, this);
        }
        ScalarProvider<Object, Object> scalarProvider = sqlClient.getScalarProvider(prop);
        if (scalarProvider != null) {
            return scalarProviderReader(scalarProvider);
        }
        if (sqlClient.getDialect().isArraySupported()) {
            Class<?> returnClass = prop.getReturnClass();
            if (prop.getAnnotation(Serialized.class) == null && (returnClass == List.class || returnClass == Collection.class)) {
                Type genericType = prop.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    Type argumentType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                    if (argumentType instanceof Class<?>) {
                        Reader<?> reader = SIMPLE_LIST_READER_MAP.get((Class<?>) argumentType);
                        if (reader != null) {
                            return reader;
                        }
                    }
                }
            }
        }
        return scalarReader(prop.getReturnClass());
    }

    private Reader<?> scalarReader(Class<?> type) {
        ScalarProvider<?, ?> scalarProvider = sqlClient.getScalarProvider(type);
        if (scalarProvider != null) {
            return scalarProviderReader(scalarProvider);
        }
        ImmutableType immutableType = ImmutableType.tryGet(type);
        if (immutableType != null && immutableType.isEmbeddable()) {
            return new FixedEmbeddedReader(immutableType, this);
        }
        Reader<?> reader = baseReader(type);
        if (reader == null) {
            throw new IllegalArgumentException(
                    "No scalar provider for customized scalar type \"" +
                            type.getName() +
                            "\""
            );
        }
        return reader;
    }

    @SuppressWarnings("unchecked")
    private @NotNull Reader<?> scalarProviderReader(ScalarProvider<?, ?> scalarProvider) {
        Reader<?> reader;
        if (scalarProvider.isJsonScalar()) {
            reader = scalarProvider.reader();
            if (reader == null) {
                reader = new JsonReader(sqlClient.getDialect());
            }
        } else {
            Class<?> sqlType = scalarProvider.getSqlType();
            reader = baseReader(sqlType);
            if (reader == null) {
                reader = unknownSqlTypeReader(sqlType, scalarProvider, sqlClient.getDialect());
            }
        }
        return new CustomizedScalarReader<>(
                (ScalarProvider<Object, Object>) scalarProvider,
                (Reader<Object>) reader
        );
    }

    private Reader<?> baseReader(Class<?> type) {
        if (type.isArray()) {
            return type == byte[].class || sqlClient.getDialect().isArraySupported() ?
                    BASE_READER_MAP.get(type) :
                    null;
        }
        return BASE_READER_MAP.get(type);
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

    private static abstract class SingleColumnReader<T> implements Reader<T> {
        @Override
        public void skip(Context ctx) {
            ctx.col();
        }
    }

    private static class ByteArrayReader extends SingleColumnReader<byte[]> {

        @Override
        public byte[] read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getBytes(ctx.col());
        }
    }

    private static class BoxedByteArrayReader extends SingleColumnReader<Byte[]> {

        @Override
        public Byte[] read(ResultSet rs, Context ctx) throws SQLException {
            return ctx.getDialect().getArray(rs, ctx.col(), Byte[].class);
        }
    }

    private static class ByteListReader extends SingleColumnReader<List<Byte>> {

        @Override
        public List<Byte> read(ResultSet rs, Context ctx) throws SQLException {
            return CollectionUtils.toListOrNull(ctx.getDialect().getArray(rs, ctx.col(), Byte[].class));
        }
    }

    private static class ShortArrayReader extends SingleColumnReader<short[]> {

        @Override
        public short[] read(ResultSet rs, Context ctx) throws SQLException {
            return toPrimitive(ctx.getDialect().getArray(rs, ctx.col(), Short[].class));
        }
    }

    private static class BoxedShortArrayReader extends SingleColumnReader<Short[]> {

        @Override
        public Short[] read(ResultSet rs, Context ctx) throws SQLException {
            return ctx.getDialect().getArray(rs, ctx.col(), Short[].class);
        }
    }

    private static class ShortListReader extends SingleColumnReader<List<Short>> {

        @Override
        public List<Short> read(ResultSet rs, Context ctx) throws SQLException {
            return CollectionUtils.toListOrNull(ctx.getDialect().getArray(rs, ctx.col(), Short[].class));
        }
    }

    private static class IntArrayReader extends SingleColumnReader<int[]> {

        @Override
        public int[] read(ResultSet rs, Context ctx) throws SQLException {
            return toPrimitive(ctx.getDialect().getArray(rs, ctx.col(), Integer[].class));
        }
    }

    private static class BoxedIntArrayReader extends SingleColumnReader<Integer[]> {

        @Override
        public Integer[] read(ResultSet rs, Context ctx) throws SQLException {
            return ctx.getDialect().getArray(rs, ctx.col(), Integer[].class);
        }
    }

    private static class IntListReader extends SingleColumnReader<List<Integer>> {

        @Override
        public List<Integer> read(ResultSet rs, Context ctx) throws SQLException {
            return CollectionUtils.toListOrNull(ctx.getDialect().getArray(rs, ctx.col(), Integer[].class));
        }
    }

    private static class LongArrayReader extends SingleColumnReader<long[]> {

        @Override
        public long[] read(ResultSet rs, Context ctx) throws SQLException {
            return toPrimitive(ctx.getDialect().getArray(rs, ctx.col(), Long[].class));
        }
    }

    private static class BoxedLongArrayReader extends SingleColumnReader<Long[]> {

        @Override
        public Long[] read(ResultSet rs, Context ctx) throws SQLException {
            return ctx.getDialect().getArray(rs, ctx.col(), Long[].class);
        }
    }

    private static class LongListReader extends SingleColumnReader<List<Long>> {

        @Override
        public List<Long> read(ResultSet rs, Context ctx) throws SQLException {
            return CollectionUtils.toListOrNull(ctx.getDialect().getArray(rs, ctx.col(), Long[].class));
        }
    }

    private static class FloatArrayReader extends SingleColumnReader<float[]> {

        @Override
        public float[] read(ResultSet rs, Context ctx) throws SQLException {
            return toPrimitive(ctx.getDialect().getArray(rs, ctx.col(), Float[].class));
        }
    }

    private static class BoxedFloatArrayReader extends SingleColumnReader<Float[]> {

        @Override
        public Float[] read(ResultSet rs, Context ctx) throws SQLException {
            return ctx.getDialect().getArray(rs, ctx.col(), Float[].class);
        }
    }

    private static class FloatListReader extends SingleColumnReader<List<Float>> {

        @Override
        public List<Float> read(ResultSet rs, Context ctx) throws SQLException {
            return CollectionUtils.toListOrNull(ctx.getDialect().getArray(rs, ctx.col(), Float[].class));
        }
    }

    private static class DoubleArrayReader extends SingleColumnReader<double[]> {

        @Override
        public double[] read(ResultSet rs, Context ctx) throws SQLException {
            return toPrimitive(ctx.getDialect().getArray(rs, ctx.col(), Double[].class));
        }
    }

    private static class BoxedDoubleArrayReader extends SingleColumnReader<Double[]> {

        @Override
        public Double[] read(ResultSet rs, Context ctx) throws SQLException {
            return ctx.getDialect().getArray(rs, ctx.col(), Double[].class);
        }
    }

    private static class DoubleListReader extends SingleColumnReader<List<Double>> {

        @Override
        public List<Double> read(ResultSet rs, Context ctx) throws SQLException {
            return CollectionUtils.toListOrNull(ctx.getDialect().getArray(rs, ctx.col(), Double[].class));
        }
    }

    private static class StringArrayReader extends SingleColumnReader<String[]> {

        @Override
        public String[] read(ResultSet rs, Context ctx) throws SQLException {
            return ctx.getDialect().getArray(rs, ctx.col(), String[].class);
        }
    }

    private static class StringListReader extends SingleColumnReader<List<String>> {

        @Override
        public List<String> read(ResultSet rs, Context ctx) throws SQLException {
            return CollectionUtils.toListOrNull(ctx.getDialect().getArray(rs, ctx.col(), String[].class));
        }
    }

    private static class UUIDArrayReader extends SingleColumnReader<UUID[]> {

        @Override
        public UUID[] read(ResultSet rs, Context ctx) throws SQLException {
            return ctx.getDialect().getArray(rs, ctx.col(), UUID[].class);
        }
    }

    private static class UUIDListReader extends SingleColumnReader<List<UUID>> {

        @Override
        public List<UUID> read(ResultSet rs, Context ctx) throws SQLException {
            return CollectionUtils.toListOrNull(ctx.getDialect().getArray(rs, ctx.col(), UUID[].class));
        }
    }

    private static class BooleanReader extends SingleColumnReader<Boolean> {

        @Override
        public Boolean read(ResultSet rs, Context ctx) throws SQLException {
            boolean value = rs.getBoolean(ctx.col());
            if (!value && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class CharReader extends SingleColumnReader<Character> {

        @Override
        public Character read(ResultSet rs, Context ctx) throws SQLException {
            String str = rs.getString(ctx.col());
            return str != null ? str.charAt(0) : null;
        }
    }

    private static class ByteReader extends SingleColumnReader<Byte> {

        @Override
        public Byte read(ResultSet rs, Context ctx) throws SQLException {
            byte value = rs.getByte(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class ShortReader extends SingleColumnReader<Short> {

        @Override
        public Short read(ResultSet rs, Context ctx) throws SQLException {
            short value = rs.getShort(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class IntReader extends SingleColumnReader<Integer> {

        @Override
        public Integer read(ResultSet rs, Context ctx) throws SQLException {
            int value = rs.getInt(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class LongReader extends SingleColumnReader<Long> {

        @Override
        public Long read(ResultSet rs, Context ctx) throws SQLException {
            long value = rs.getLong(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class FloatReader extends SingleColumnReader<Float> {

        @Override
        public Float read(ResultSet rs, Context ctx) throws SQLException {
            float value = rs.getFloat(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class DoubleReader extends SingleColumnReader<Double> {

        @Override
        public Double read(ResultSet rs, Context ctx) throws SQLException {
            double value = rs.getDouble(ctx.col());
            if (value == 0 && rs.wasNull()) {
                return null;
            }
            return value;
        }
    }

    private static class BigIntegerReader extends SingleColumnReader<BigInteger> {

        @Override
        public BigInteger read(ResultSet rs, Context ctx) throws SQLException {
            BigDecimal decimal = rs.getBigDecimal(ctx.col());
            return decimal != null ? decimal.toBigInteger() : null;
        }
    }

    private static class BigDecimalReader extends SingleColumnReader<BigDecimal> {

        @Override
        public BigDecimal read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getBigDecimal(ctx.col());
        }
    }

    private static class StringReader extends SingleColumnReader<String> {

        @Override
        public String read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getString(ctx.col());
        }
    }

    private static class UUIDReader extends SingleColumnReader<UUID> {

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

    private static class BlobReader extends SingleColumnReader<Blob> {

        @Override
        public Blob read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getBlob(ctx.col());
        }
    }

    private static class SqlDateReader extends SingleColumnReader<java.sql.Date> {

        @Override
        public java.sql.Date read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getDate(ctx.col());
        }
    }

    private static class SqlTimeReader extends SingleColumnReader<java.sql.Time> {

        @Override
        public java.sql.Time read(ResultSet rs, Context ctx) throws SQLException {
            return rs.getTime(ctx.col());
        }
    }

    private static class SqlTimestampReader extends SingleColumnReader<java.sql.Timestamp> {

        @Override
        public Timestamp read(ResultSet rs, Context ctx) throws SQLException {
            return ctx.getDialect().getTimestamp(rs, ctx.col());
        }
    }

    private static class DateReader extends SingleColumnReader<java.util.Date> {
        @Override
        public java.util.Date read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = ctx.getDialect().getTimestamp(rs, ctx.col());
            return timestamp != null ?
                    java.util.Date.from(timestamp.toInstant()) :
                    null;
        }
    }

    private static class LocalDateReader extends SingleColumnReader<LocalDate> {

        @Override
        public LocalDate read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = ctx.getDialect().getTimestamp(rs, ctx.col());
            return timestamp != null ?
                    LocalDateTime.ofInstant(timestamp.toInstant(), ctx.getZoneId()).toLocalDate() :
                    null;
        }
    }

    private static class LocalTimeReader extends SingleColumnReader<LocalTime> {

        @Override
        public LocalTime read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = ctx.getDialect().getTimestamp(rs, ctx.col());
            return timestamp != null ?
                    LocalDateTime.ofInstant(timestamp.toInstant(), ctx.getZoneId()).toLocalTime() :
                    null;
        }
    }

    private static class LocalDateTimeReader extends SingleColumnReader<LocalDateTime> {

        @Override
        public LocalDateTime read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = ctx.getDialect().getTimestamp(rs, ctx.col());
            return timestamp != null ?
                    LocalDateTime.ofInstant(timestamp.toInstant(), ctx.getZoneId()) :
                    null;
        }
    }

    private static class OffsetDateTimeReader extends SingleColumnReader<OffsetDateTime> {

        @Override
        public OffsetDateTime read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = ctx.getDialect().getTimestamp(rs, ctx.col());
            return timestamp != null ?
                    OffsetDateTime.ofInstant(timestamp.toInstant(), ctx.getZoneId()) :
                    null;
        }
    }

    private static class ZonedDateTimeReader extends SingleColumnReader<ZonedDateTime> {

        @Override
        public ZonedDateTime read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = ctx.getDialect().getTimestamp(rs, ctx.col());
            return timestamp != null ?
                    ZonedDateTime.ofInstant(timestamp.toInstant(), ctx.getZoneId()) :
                    null;
        }
    }

    private static class InstantReader extends SingleColumnReader<Instant> {

        @Override
        public Instant read(ResultSet rs, Context ctx) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(ctx.col());
            return timestamp != null ? timestamp.toInstant() : null;
        }
    }

    private static class JsonReader extends SingleColumnReader<String> {

        private final Dialect dialect;

        private JsonReader(Dialect dialect) {
            this.dialect = dialect;
        }

        @Override
        public String read(ResultSet rs, Context ctx) throws SQLException {
            return dialect.baseValueToJson(rs.getObject(ctx.col(), dialect.getJsonBaseType()));
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
                                "\" by scalar provider \"" +
                                scalarProvider +
                                "\"",
                        ex
                );
            }
        }

        @Override
        public void skip(Context ctx) {
            sqlReader.skip(ctx);
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

        @Override
        public void skip(Context ctx) {
            foreignKeyReader.skip(ctx);
        }
    }

    private static class ReferenceIdViewReader implements Reader<Object> {

        private final Reader<?> foreignKeyReader;

        private ReferenceIdViewReader(ImmutableProp prop, ReaderManager readerManager) {
            this.foreignKeyReader = readerManager.scalarReader(
                    prop.getIdViewBaseProp().getTargetType().getIdProp()
            );
        }

        @Override
        public Object read(ResultSet rs, Context ctx) throws SQLException {
            return foreignKeyReader.read(rs, ctx);
        }

        @Override
        public void skip(Context ctx) {
            foreignKeyReader.skip(ctx);
        }
    }

    private static class AssociationReader implements Reader<Association<?, ?>> {

        private final Reader<?> sourceReader;

        private final Reader<?> targetReader;

        AssociationReader(AssociationType associationType, ReaderManager readerManager) {
            sourceReader = new ReferenceReader(associationType.getSourceProp(), readerManager);
            targetReader = new ReferenceReader(associationType.getTargetProp(), readerManager);
        }

        @Override
        public Association<?, ?> read(ResultSet rs, Context ctx) throws SQLException {
            Object source = sourceReader.read(rs, ctx);
            Object target = targetReader.read(rs, ctx);
            return new Association<>(source, target);
        }

        @Override
        public void skip(Context ctx) {
            sourceReader.skip(ctx);
            targetReader.skip(ctx);
        }
    }

    private static class FixedEmbeddedReader implements Reader<Object> {

        private static final ImmutableProp[] EMPTY_PROPS = new ImmutableProp[0];

        private static final Reader<?>[] EMPTY_READERS = new Reader[0];

        private final ImmutableType targetType;

        private ImmutableProp[] props;

        private Reader<?>[] readers;

        FixedEmbeddedReader(ImmutableType targetType, ReaderManager readerManager) {
            this.targetType = targetType;
            Map<ImmutableProp, Reader<?>> map = new LinkedHashMap<>();
            for (ImmutableProp childProp : targetType.getProps().values()) {
                if (childProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                    map.put(childProp, new FixedEmbeddedReader(childProp.getTargetType(), readerManager));
                } else if (!childProp.isFormula()) {
                    assert childProp.getSqlTemplate() == null; // SQL formula is not supported by embeddable
                    map.put(childProp, readerManager.scalarReader(childProp));
                }
            }
            props = map.keySet().toArray(EMPTY_PROPS);
            readers = map.values().toArray(EMPTY_READERS);
        }

        @Override
        public Object read(ResultSet rs, Context ctx) throws SQLException {
            DraftSpi spi = (DraftSpi) targetType.getDraftFactory().apply(ctx.draftContext(), null);
            boolean hasNoNull = false;
            boolean hasRequiredNull = false;
            try {
                int size = readers.length;
                for (int i = 0; i < size; i++) {
                    Object value = readers[i].read(rs, ctx);
                    if (hasRequiredNull) {
                        continue;
                    }
                    ImmutableProp prop = props[i];
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
                return DraftConsumerUncheckedException.rethrow(ex);
            }
            return hasNoNull && !hasRequiredNull ? ctx.resolve(spi) : null;
        }

        @Override
        public void skip(Context ctx) {
            for (Reader<?> reader : readers) {
                reader.skip(ctx);
            }
        }
    }

    private static boolean[] toPrimitive(Boolean[] arr) {
        if (arr == null) {
            return null;
        }
        boolean[] newArr = new boolean[arr.length];
        for (int i = arr.length - 1; i >= 0; --i) {
            newArr[i] = arr[i];
        }
        return newArr;
    }

    private static short[] toPrimitive(Short[] arr) {
        if (arr == null) {
            return null;
        }
        short[] newArr = new short[arr.length];
        for (int i = arr.length - 1; i >= 0; --i) {
            newArr[i] = arr[i];
        }
        return newArr;
    }

    private static int[] toPrimitive(Integer[] arr) {
        if (arr == null) {
            return null;
        }
        int[] newArr = new int[arr.length];
        for (int i = arr.length - 1; i >= 0; --i) {
            newArr[i] = arr[i];
        }
        return newArr;
    }

    private static long[] toPrimitive(Long[] arr) {
        if (arr == null) {
            return null;
        }
        long[] newArr = new long[arr.length];
        for (int i = arr.length - 1; i >= 0; --i) {
            newArr[i] = arr[i];
        }
        return newArr;
    }

    private static float[] toPrimitive(Float[] arr) {
        if (arr == null) {
            return null;
        }
        float[] newArr = new float[arr.length];
        for (int i = arr.length - 1; i >= 0; --i) {
            newArr[i] = arr[i];
        }
        return newArr;
    }

    private static double[] toPrimitive(Double[] arr) {
        if (arr == null) {
            return null;
        }
        double[] newArr = new double[arr.length];
        for (int i = arr.length - 1; i >= 0; --i) {
            newArr[i] = arr[i];
        }
        return newArr;
    }

    static {
        Map<Class<?>, Reader<?>> baseReaderMap = new HashMap<>();
        baseReaderMap.put(boolean.class, new BooleanReader());
        baseReaderMap.put(Boolean.class, new BooleanReader());
        baseReaderMap.put(char.class, new CharReader());
        baseReaderMap.put(Character.class, new CharReader());
        baseReaderMap.put(byte.class, new ByteReader());
        baseReaderMap.put(Byte.class, new ByteReader());
        baseReaderMap.put(byte[].class, new ByteArrayReader());
        baseReaderMap.put(Byte[].class, new BoxedByteArrayReader());
        baseReaderMap.put(short.class, new ShortReader());
        baseReaderMap.put(Short.class, new ShortReader());
        baseReaderMap.put(short[].class, new ShortArrayReader());
        baseReaderMap.put(Short[].class, new BoxedShortArrayReader());
        baseReaderMap.put(int.class, new IntReader());
        baseReaderMap.put(Integer.class, new IntReader());
        baseReaderMap.put(int[].class, new IntArrayReader());
        baseReaderMap.put(Integer[].class, new BoxedIntArrayReader());
        baseReaderMap.put(long.class, new LongReader());
        baseReaderMap.put(Long.class, new LongReader());
        baseReaderMap.put(long[].class, new LongArrayReader());
        baseReaderMap.put(Long[].class, new BoxedLongArrayReader());
        baseReaderMap.put(float.class, new FloatReader());
        baseReaderMap.put(Float.class, new FloatReader());
        baseReaderMap.put(float[].class, new FloatArrayReader());
        baseReaderMap.put(Float[].class, new BoxedFloatArrayReader());
        baseReaderMap.put(double.class, new DoubleReader());
        baseReaderMap.put(Double.class, new DoubleReader());
        baseReaderMap.put(double[].class, new DoubleArrayReader());
        baseReaderMap.put(Double[].class, new BoxedDoubleArrayReader());
        baseReaderMap.put(BigInteger.class, new BigIntegerReader());
        baseReaderMap.put(BigDecimal.class, new BigDecimalReader());
        baseReaderMap.put(String.class, new StringReader());
        baseReaderMap.put(String[].class, new StringArrayReader());
        baseReaderMap.put(UUID.class, new UUIDReader());
        baseReaderMap.put(UUID[].class, new UUIDArrayReader());
        baseReaderMap.put(Blob.class, new BlobReader());
        baseReaderMap.put(java.sql.Date.class, new SqlDateReader());
        baseReaderMap.put(java.sql.Time.class, new SqlTimeReader());
        baseReaderMap.put(java.sql.Timestamp.class, new SqlTimestampReader());
        baseReaderMap.put(java.util.Date.class, new DateReader());
        baseReaderMap.put(LocalDate.class, new LocalDateReader());
        baseReaderMap.put(LocalTime.class, new LocalTimeReader());
        baseReaderMap.put(LocalDateTime.class, new LocalDateTimeReader());
        baseReaderMap.put(OffsetDateTime.class, new OffsetDateTimeReader());
        baseReaderMap.put(ZonedDateTime.class, new ZonedDateTimeReader());
        baseReaderMap.put(Instant.class, new InstantReader());
        BASE_READER_MAP = baseReaderMap;

        Map<Class<?>, Reader<?>> simpleListReaderMap = new HashMap<>();
        simpleListReaderMap.put(Byte.class, new ByteListReader());
        simpleListReaderMap.put(Short.class, new ShortListReader());
        simpleListReaderMap.put(Integer.class, new IntListReader());
        simpleListReaderMap.put(Long.class, new LongListReader());
        simpleListReaderMap.put(Float.class, new FloatListReader());
        simpleListReaderMap.put(Double.class, new DoubleListReader());
        simpleListReaderMap.put(String.class, new StringListReader());
        simpleListReaderMap.put(UUID.class, new UUIDListReader());
        SIMPLE_LIST_READER_MAP = simpleListReaderMap;
    }
}
