package org.babyfish.jimmer.sql.runtime;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.UUID;

public abstract class AbstractScalarProvider<T, S> implements ScalarProvider<T, S> {

    static final ScalarProvider<UUID, byte[]> UUID_BY_BYTE_ARRAY =
            new AbstractScalarProvider<UUID, byte[]>() {
                @Override
                public UUID toScalar(byte[] sqlValue) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(sqlValue);
                    long high = byteBuffer.getLong();
                    long low = byteBuffer.getLong();
                    return new UUID(high, low);
                }

                @Override
                public byte[] toSql(UUID scalarValue) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
                    byteBuffer.putLong(scalarValue.getMostSignificantBits());
                    byteBuffer.putLong(scalarValue.getLeastSignificantBits());
                    return byteBuffer.array();
                }

                @Override
                public String toString() {
                    return "ScalarProvider.UUID_BY_BYTE_ARRAY";
                }
            };

    static final ScalarProvider<UUID, String> UUID_BY_STRING =
            new AbstractScalarProvider<UUID, String>() {
                @Override
                public UUID toScalar(String sqlValue) {
                    return UUID.fromString(sqlValue);
                }

                @Override
                public String toSql(UUID scalarValue) {
                    return scalarValue.toString();
                }

                @Override
                public String toString() {
                    return "ScalarProvider.UUID_BY_STRING";
                }
            };

    private final Type scalarType;

    private final Class<S> sqlType;

    public AbstractScalarProvider(Class<T> scalarType, Class<S> sqlType) {
        Meta.validateScalarType(scalarType);
        this.scalarType = scalarType;
        this.sqlType = sqlType;
    }

    @SuppressWarnings("unchecked")
    protected AbstractScalarProvider() {
        Meta meta = Meta.create(this.getClass());
        this.scalarType = meta.scalarType;
        this.sqlType = (Class<S>) meta.sqlType;
    }

    @NotNull
    public final Type getScalarType() {
        return scalarType;
    }

    @NotNull
    public final Class<S> getSqlType() {
        return sqlType;
    }
}
