package org.babyfish.jimmer.sql.runtime;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractScalarProvider<T, S> implements ScalarProvider<T, S> {

    static final ClassCache<Meta> META_CACHE =
            new ClassCache<>(AbstractScalarProvider::createMeta);

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
            };

    private final Type scalarType;

    private final Class<S> sqlType;

    public AbstractScalarProvider(Class<T> scalarType, Class<S> sqlType) {
        validateScalarType(scalarType);
        this.scalarType = scalarType;
        this.sqlType = sqlType;
    }

    @SuppressWarnings("unchecked")
    protected AbstractScalarProvider() {
        Meta meta = createMeta(this.getClass());
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

    private static Meta createMeta(Class<?> scalarProviderType) {
        Map<TypeVariable<?>, Type> argMap = TypeUtils.getTypeArguments(scalarProviderType, ScalarProvider.class);
        if (argMap.isEmpty()) {
            throw new IllegalStateException(
                    "Illegal type \"" +
                            scalarProviderType.getName() +
                            "\", it does not specify generic arguments for \"" +
                            ScalarProvider.class.getName() +
                            "\""
            );
        }
        TypeVariable<?>[] params = ScalarProvider.class.getTypeParameters();
        Type scalarType = argMap.get(params[0]);
        Class<?> sqlType = (Class<?>) argMap.get(params[1]);
        validateScalarType(scalarType);
        return new Meta(scalarType, sqlType);
    }

    private static void validateScalarType(Type scalarType) {

        if (scalarType == UUID.class) {
            return; // UUID is standard type, but it can be overridden by ScalarProvider
        }

        if (!(scalarType instanceof Class<?>)) {
            return;
        }
        Class<?> scalarClass = (Class<?>) scalarType;

        if (scalarType == void.class) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            scalarClass.getName() +
                            "\", it cannot be void"
            );
        }
        if (scalarType == Object.class) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            scalarClass.getName() +
                            "\", scalar provider does not support object type which means any"
            );
        }
        if (TupleImplementor.class.isAssignableFrom(scalarClass)) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            scalarClass.getName() +
                            "\", scalar provider does not support tuple type"
            );
        }
        if (ReaderManager.isStandardScalarType(scalarClass)) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            ((Class<?>)scalarType).getName() +
                            "\", scalar provider does not support standard scalar type"
            );
        }
        Class<?> annotationType = getOrmAnnotationType(scalarClass);
        if (annotationType != null) {
            throw new IllegalArgumentException(
                    "Illegal scalar type \"" +
                            scalarClass.getName() +
                            "\", scalar provider does not support scalar type which is decorated by \"@" +
                            annotationType.getName() +
                            "\""
            );
        }
    }

    private static Class<?> getOrmAnnotationType(Class<?> type) {
        if (type == null) {
            return null;
        }
        if (type != Object.class) {
            if (type.isAnnotationPresent(Entity.class)) {
                return Entity.class;
            }
            if (type.isAssignableFrom(MappedSuperclass.class)) {
                return MappedSuperclass.class;
            }
            if (type.isAssignableFrom(Embeddable.class)) {
                return Embeddable.class;
            }
        }
        Class<?> annoType = getOrmAnnotationType(type.getSuperclass());
        if (annoType != null) {
            return annoType;
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            annoType = getOrmAnnotationType(interfaceType);
            if (annoType != null) {
                return annoType;
            }
        }
        return null;
    }

    static class Meta {

        final Type scalarType;

        final Class<?> sqlType;

        private Meta(Type scalarType, Class<?> sqlType) {
            this.scalarType = scalarType;
            this.sqlType = sqlType;
        }
    }
}
