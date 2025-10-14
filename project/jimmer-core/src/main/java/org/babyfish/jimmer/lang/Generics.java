package org.babyfish.jimmer.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * After upgrade to Java25, the `TypeUtils` of `common-lang3`
 * may cause errors, use this class to replace it.
 */
public class Generics {

    private static final TypeVariable<?>[] EMPTY_TYPE_PARAMETERS =
            new TypeVariable[0];

    private static final Type[] EMPTY_TYPE_ARGUMENTS =
            new Type[0];

    private Generics() {}

    @NotNull
    public static GenericArrayType makeGenericArrayType(
            @NotNull Type componentType
    ) {
        return new GenericArrayTypeImpl(componentType);
    }

    @NotNull
    public static WildcardType makeWildcardType(
            @Nullable Type[] upperBounds,
            @Nullable Type[] lowerBounds
    ) {
        return new WildcardTypeImpl(upperBounds, lowerBounds);
    }

    @NotNull
    public static ParameterizedType makeParameterizedType(
            @NotNull Class<?> rawType,
            @NotNull Type... arguments
    ) {
        return new ParameterizedTypeImpl(rawType, rawType.getEnclosingClass(), arguments);
    }

    @NotNull
    public static ParameterizedType makeParameterizedType(
            @NotNull Type rawType,
            @Nullable Type ownerType,
            @NotNull Type[] arguments
    ) {
        return new ParameterizedTypeImpl(rawType, ownerType, arguments);
    }

    @NotNull
    public static Type[] getTypeArguments(
            @NotNull Type type,
            @NotNull Class<?> toClass
    ) {
        TypeVariable<?>[] typeParameters = toClass.getTypeParameters();
        if (typeParameters.length == 0) {
            return new Type[0];
        }
        ResolveContext ctx = findCtx(new ResolveContext(type, toClass, null));
        if (ctx == null) {
            return toClass.getTypeParameters();
        }
        Type[] argumentTypes = new Type[typeParameters.length];
        for (int i = typeParameters.length - 1; i >= 0; --i) {
            argumentTypes[i] = ctx.resolve(typeParameters[i]);
        }
        return argumentTypes;
    }

    private static ResolveContext findCtx(ResolveContext ctx) {
        Class<?> cls;
        if (ctx.type instanceof Class<?>) {
            cls = (Class<?>) ctx.type;
        } else {
            cls = (Class<?>) ((ParameterizedType)ctx.type).getRawType();
        }
        if (cls == ctx.toClass) {
            return ctx;
        }
        Type superType = cls.getGenericSuperclass();
        if (superType != null && superType != Object.class) {
            ResolveContext baseCtx = findCtx(ctx.base(superType));
            if (baseCtx != null) {
                return baseCtx;
            }
        }
        for (Type superItf : cls.getGenericInterfaces()) {
            ResolveContext baseCtx = findCtx(ctx.base(superItf));
            if (baseCtx != null) {
                return baseCtx;
            }
        }
        return null;
    }

    private static class ResolveContext {

        final Type type;

        final Class<?> toClass;

        final ResolveContext parent;

        private TypeVariable<?>[] _typeParameters;

        private Type[] _typeArguments;

        ResolveContext(Type type, Class<?> toClass, ResolveContext parent) {
            this.type = type;
            this.toClass = toClass;
            this.parent = parent;
        }

        ResolveContext base(Type type) {
            return new ResolveContext(type, toClass, this);
        }

        Type resolve(Type type) {
            while (true) {
                Type resolvedType = resolve0(type);
                if (resolvedType == type) {
                    return type;
                }
                type = resolvedType;
            }
        }

        private Type resolve0(Type type) {
            if (type instanceof GenericArrayType) {
                Type componentType = ((GenericArrayType) type).getGenericComponentType();
                Type resolvedComponentType = resolve0(componentType);
                return componentType == resolvedComponentType ?
                        type :
                        resolvedComponentType instanceof Class<?> ?
                                Array.newInstance((Class<?>) resolvedComponentType, 0).getClass() :
                                new GenericArrayTypeImpl(resolvedComponentType);
            }
            if (type instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) type;
                boolean changed = false;
                Type[] upperBounds = wildcardType.getUpperBounds();
                Type[] resolvedUpperBounds = new Type[upperBounds.length];
                for (int i = upperBounds.length - 1; i >= 0; --i) {
                    resolvedUpperBounds[i] = resolve0(upperBounds[i]);
                    changed |= resolvedUpperBounds[i] != upperBounds[i];
                }
                Type[] lowerBounds = wildcardType.getLowerBounds();
                Type[] resolvedLowerBounds = new Type[lowerBounds.length];
                for (int i = lowerBounds.length - 1; i >= 0; --i) {
                    resolvedLowerBounds[i] = resolve0(lowerBounds[i]);
                    changed |= resolvedLowerBounds[i] != lowerBounds[i];
                }
                if (!changed) {
                    return type;
                }
                return new WildcardTypeImpl(upperBounds, lowerBounds);
            }
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                boolean changed = false;
                Type[] argumentTypes = parameterizedType.getActualTypeArguments();
                Type[] resolvedArgumentTypes = new Type[argumentTypes.length];
                for (int i = argumentTypes.length - 1; i >= 0; --i) {
                    resolvedArgumentTypes[i] = resolve0(argumentTypes[i]);
                    changed |= resolvedArgumentTypes[i] != argumentTypes[i];
                }
                if (!changed) {
                    return type;
                }
                return new ParameterizedTypeImpl(
                        parameterizedType.getRawType(),
                        parameterizedType.getOwnerType(),
                        resolvedArgumentTypes
                );
            }
            return resolve1(type);
        }

        private Type resolve1(Type type) {
            if (type instanceof TypeVariable<?>) {
                for (ResolveContext ctx = this; ctx != null; ctx = ctx.parent) {
                    for (int i = ctx.typeParameters().length - 1; i >= 0; --i) {
                        if (ctx.typeParameters()[i].equals(type)) {
                            return ctx.typeArguments()[i];
                        }
                    }
                }
            }
            return type;
        }

        private TypeVariable<?>[] typeParameters() {
            lazyInit();
            return _typeParameters;
        }

        private Type[] typeArguments() {
            lazyInit();
            return _typeArguments;
        }

        private void lazyInit() {
            if (_typeArguments != null) {
                return;
            }
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                if (parameterizedType.getOwnerType() instanceof ParameterizedType) {
                    throw new UnsupportedOperationException(
                            "\"" +
                                    Generics.class.getName() +
                                    "\" does not support \"" +
                                    type.getTypeName() +
                                    "\", which is parameterized type with owner type"
                    );
                }
                _typeParameters = ((Class<?>)parameterizedType.getRawType()).getTypeParameters();
                _typeArguments = parameterizedType.getActualTypeArguments();
            } else {
                _typeParameters = EMPTY_TYPE_PARAMETERS;
                _typeArguments = EMPTY_TYPE_ARGUMENTS;
            }
        }
    }
    
    private static class GenericArrayTypeImpl implements GenericArrayType {

        private final Type genericComponentType;

        GenericArrayTypeImpl(Type genericComponentType) {
            this.genericComponentType = genericComponentType;
        }

        @NotNull
        @Override
        public Type getGenericComponentType() {
            return genericComponentType;
        }

        @Override
        public String getTypeName() {
            return genericComponentType.getTypeName() + "[]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GenericArrayType)) {
                return false;
            }
            GenericArrayType that = (GenericArrayType) o;
            return genericComponentType.equals(that.getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return genericComponentType.hashCode();
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }
    
    private static class WildcardTypeImpl implements WildcardType {

        private final Type[] upperBounds;
        
        private final Type[] lowerBounds;

        private WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
            this.upperBounds = upperBounds != null ? upperBounds : EMPTY_TYPE_ARGUMENTS;
            this.lowerBounds = lowerBounds != null ? lowerBounds : EMPTY_TYPE_ARGUMENTS;
        }

        @Override
        @NotNull
        public Type[] getUpperBounds() {
            return upperBounds.clone();
        }

        @Override
        @NotNull
        public Type[] getLowerBounds() {
            return lowerBounds.clone();
        }

        @Override
        public String getTypeName() {
            StringBuilder builder = new StringBuilder();
            builder.append('?');
            if (lowerBounds.length != 0) {
                builder.append(" super ");
                boolean addAnd = false;
                for (Type lowerBound : lowerBounds) {
                    if (addAnd) {
                        builder.append(" & ");
                    } else {
                        addAnd = true;
                    }
                    builder.append(lowerBound.getTypeName());
                }
            }
            if (upperBounds.length != 0 && (upperBounds.length != 1 || !upperBounds[0].equals(Object.class))) {
                builder.append(" extends ");
                boolean addAnd = false;
                for (Type upperBound : upperBounds) {
                    if (addAnd) {
                        builder.append(" & ");
                    } else {
                        addAnd = true;
                    }
                    builder.append(upperBound.getTypeName());
                }
            }
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof WildcardType)) {
                return false;
            }
            WildcardType that = (WildcardType) o;
            return Objects.deepEquals(upperBounds, that.getUpperBounds()) &&
                    Objects.deepEquals(lowerBounds, that.getLowerBounds());
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(upperBounds), Arrays.hashCode(lowerBounds));
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {

        private final Type rawType;

        private final Type ownerType;

        private final Type[] actualTypeArguments;

        private ParameterizedTypeImpl(Type rawType, Type ownerType, Type[] actualTypeArguments) {
            this.rawType = rawType;
            this.ownerType = ownerType;
            this.actualTypeArguments =
                    actualTypeArguments != null ?
                            actualTypeArguments :
                            EMPTY_TYPE_ARGUMENTS;
        }

        @Override
        @NotNull
        public Type[] getActualTypeArguments() {
            return actualTypeArguments.clone();
        }

        @NotNull
        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public String getTypeName() {
            StringBuilder builder = new StringBuilder();
            builder.append(rawType.getTypeName());
            builder.append("<");
            boolean addComma = false;
            for (Type argument : actualTypeArguments) {
                if (addComma) {
                    builder.append(", ");
                } else {
                    addComma = true;
                }
                builder.append(argument.getTypeName());
            }
            builder.append(">");
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType that = (ParameterizedType) o;
            return Objects.equals(rawType, that.getRawType()) &&
                    Objects.equals(ownerType, that.getOwnerType()) &&
                    Objects.deepEquals(actualTypeArguments, that.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Objects.hash(rawType, ownerType, Arrays.hashCode(actualTypeArguments));
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }
}
