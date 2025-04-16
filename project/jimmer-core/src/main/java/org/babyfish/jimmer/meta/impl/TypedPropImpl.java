package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;

public class TypedPropImpl<S, T> implements TypedProp<S, T> {

    protected final ImmutableProp prop;

    protected TypedPropImpl(ImmutableProp prop) {
        if (prop instanceof TypedProp<?, ?>) {
            throw new IllegalArgumentException("TypedProp can only wrap raw prop");
        }
        this.prop = prop;
    }

    @Override
    public String toString() {
        return prop.toString();
    }

    @Override
    public ImmutableProp unwrap() {
        return prop;
    }

    @Override
    public boolean isLoaded(Object immutable) {
        return immutable == null || ((ImmutableSpi)immutable).__isLoaded(prop.getId());
    }

    public static class Scalar<S, T> extends TypedPropImpl<S, T> implements TypedProp.Scalar<S, T> {

        private final boolean desc;

        private final boolean nullsFirst;

        private final boolean nullsLast;

        @SuppressWarnings("unchecked")
        public static <S, T> TypedProp.Scalar<S, T> of(ImmutableProp prop) {
            Class<?> type = prop.getReturnClass();
            boolean nullable = prop.isNullable();
            if (type == String.class) {
                return nullable ?
                        (TypedProp.Scalar<S, T>) new TypedPropImpl.StringScalar.Nullable<>(prop) :
                        (TypedProp.Scalar<S, T>) new TypedPropImpl.StringScalar.NonNull<>(prop);
            }
            if (Number.class.isAssignableFrom(type)) {
                return nullable ?
                        (TypedProp.Scalar<S, T>) new TypedPropImpl.NumericScalar.Nullable<>(prop) :
                        (TypedProp.Scalar<S, T>) new TypedPropImpl.NumericScalar.NonNull<>(prop);
            }
            if (Comparable.class.isAssignableFrom(type)) {
                return nullable ?
                        (TypedProp.Scalar<S, T>) new TypedPropImpl.ComparableScalar.Nullable<>(prop) :
                        (TypedProp.Scalar<S, T>) new TypedPropImpl.ComparableScalar.NonNull<>(prop);
            }
            return nullable ?
                    (TypedProp.Scalar<S, T>) new TypedPropImpl.Scalar.Nullable<>(prop) :
                    (TypedProp.Scalar<S, T>) new TypedPropImpl.Scalar.NonNull<>(prop);
        }

        public Scalar(ImmutableProp prop) {
            this(prop, false, false, false);
        }

        Scalar(
                ImmutableProp prop,
                boolean desc,
                boolean nullsFirst,
                boolean nullsLast
        ) {
            super(prop);
            this.desc = desc;
            this.nullsFirst = nullsFirst;
            this.nullsLast = nullsLast;
            if (!prop.isScalar(TargetLevel.OBJECT)) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not scalar property"
                );
            }
        }

        @Override
        public TypedProp.Scalar<S, T> asc() {
            if (!desc) {
                return this;
            }
            return new TypedPropImpl.Scalar<>(prop, false, nullsFirst, nullsLast);
        }

        @Override
        public TypedProp.Scalar<S, T> desc() {
            if (desc) {
                return this;
            }
            return new TypedPropImpl.Scalar<>(prop, true, nullsFirst, nullsLast);
        }

        @Override
        public TypedProp.Scalar<S, T> nullsFirst() {
            if (nullsFirst) {
                return this;
            }
            return new TypedPropImpl.Scalar<>(prop, desc, true, false);
        }

        @Override
        public TypedProp.Scalar<S, T> nullsLast() {
            if (nullsLast) {
                return this;
            }
            return new TypedPropImpl.Scalar<>(prop, desc, false, true);
        }

        @Override
        public boolean isDesc() {
            return desc;
        }

        @Override
        public boolean isNullsFirst() {
            return nullsFirst;
        }

        @Override
        public boolean isNullsLast() {
            return nullsLast;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(super.toString());
            if (desc) {
                builder.append(" desc");
            }
            if (nullsFirst) {
                builder.append(" nullsFirst");
            }
            if (nullsLast) {
                builder.append(" nullLast");
            }
            return builder.toString();
        }

        public static class NonNull<S, T>
                extends TypedPropImpl.Scalar<S, T>
                implements TypedProp.Scalar.NonNull<S, T> {

            public NonNull(ImmutableProp prop) {
                super(nonNull(prop));
            }
        }

        public static class Nullable<S, T>
                extends TypedPropImpl.Scalar<S, T>
                implements TypedProp.Scalar.Nullable<S, T> {

            public Nullable(ImmutableProp prop) {
                super(nullable(prop));
            }
        }
    }

    public static abstract class StringScalar<S> extends Scalar<S, String> implements TypedProp.StringScalar<S> {

        StringScalar(ImmutableProp prop) {
            super(prop);
            if (prop.getReturnClass() != String.class) {
                throw new IllegalArgumentException(
                        "Cannot create string scalar because \"" +
                                prop +
                                "\" does not return string"
                );
            }
        }

        public static class NonNull<S>
                extends TypedPropImpl.StringScalar<S>
                implements TypedProp.StringScalar.NonNull<S> {

            public NonNull(ImmutableProp prop) {
                super(nonNull(prop));
            }
        }

        public static class Nullable<S>
                extends TypedPropImpl.StringScalar<S>
                implements TypedProp.StringScalar.Nullable<S> {

            public Nullable(ImmutableProp prop) {
                super(nullable(prop));
            }
        }
    }

    public static abstract class NumericScalar<S, N extends Number & Comparable<N>> extends Scalar<S, N> implements TypedProp.NumricScalar<S, N> {

        NumericScalar(ImmutableProp prop) {
            super(prop);
            if (!Number.class.isAssignableFrom(prop.getReturnClass())) {
                throw new IllegalArgumentException(
                        "Cannot create numeric scalar because \"" +
                                prop +
                                "\" does not return numeric"
                );
            }
        }

        public static class NonNull<S, N extends Number & Comparable<N>>
                extends NumericScalar<S, N>
                implements TypedProp.NumricScalar.NonNull<S, N> {

            public NonNull(ImmutableProp prop) {
                super(nonNull(prop));
            }
        }

        public static class Nullable<S, N extends Number & Comparable<N>>
                extends NumericScalar<S, N>
                implements TypedProp.NumricScalar.Nullable<S, N> {

            public Nullable(ImmutableProp prop) {
                super(nullable(prop));
            }
        }
    }

    public static abstract class ComparableScalar<S, T extends Comparable<?>> extends Scalar<S, T> implements TypedProp.ComparableScalar<S, T> {

        ComparableScalar(ImmutableProp prop) {
            super(prop);
            if (!Comparable.class.isAssignableFrom(prop.getReturnClass())) {
                throw new IllegalArgumentException(
                        "Cannot create comparable scalar because \"" +
                                prop +
                                "\" does not return comparable"
                );
            }
        }

        public static class NonNull<S, T extends Comparable<?>>
                extends TypedPropImpl.ComparableScalar<S, T>
                implements TypedProp.ComparableScalar.NonNull<S, T> {

            public NonNull(ImmutableProp prop) {
                super(nonNull(prop));
            }
        }

        public static class Nullable<S, T extends Comparable<?>>
                extends TypedPropImpl.ComparableScalar<S, T>
                implements TypedProp.ComparableScalar.Nullable<S, T> {

            public Nullable(ImmutableProp prop) {
                super(nullable(prop));
            }
        }
    }

    public static abstract class ScalarList<S, T> extends TypedPropImpl<S, T> implements TypedProp.ScalarList<S, T> {

        ScalarList(ImmutableProp prop) {
            super(prop);
            if (!prop.isScalarList()) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not scalar list property"
                );
            }
        }

        public static <S, T> TypedProp.ScalarList<S, T> of(ImmutableProp prop) {
            return prop.isNullable() ?
                    new Nullable<S, T>(prop) :
                    new NonNull<S, T>(prop);
        }

        public static class NonNull<S, T>
                extends TypedPropImpl.ScalarList<S, T>
                implements TypedProp.ScalarList.NonNull<S, T> {

            public NonNull(ImmutableProp prop) {
                super(nonNull(prop));
            }
        }

        public static class Nullable<S, T>
                extends TypedPropImpl.ScalarList<S, T>
                implements TypedProp.ScalarList.Nullable<S, T> {

            public Nullable(ImmutableProp prop) {
                super(nullable(prop));
            }
        }
    }

    public static abstract class Reference<S, T> extends TypedPropImpl<S, T> implements TypedProp.Reference<S, T> {

        Reference(ImmutableProp prop) {
            super(prop);
            if (!prop.isReference(TargetLevel.OBJECT)) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not reference property"
                );
            }
        }

        public static <S, T> TypedProp.Reference<S, T> of(ImmutableProp prop) {
            return prop.isNullable() ?
                    new Nullable<S, T>(prop) :
                    new NonNull<S, T>(prop);
        }

        public static class NonNull<S, T>
                extends TypedPropImpl.Reference<S, T>
                implements TypedProp.Reference.NonNull<S, T> {

            public NonNull(ImmutableProp prop) {
                super(nonNull(prop));
            }
        }

        public static class Nullable<S, T>
                extends TypedPropImpl.Reference<S, T>
                implements TypedProp.Reference.Nullable<S, T> {

            public Nullable(ImmutableProp prop) {
                super(nullable(prop));
            }
        }
    }

    public static class ReferenceList<S, T> extends TypedPropImpl<S, T> implements TypedProp.ReferenceList<S, T> {

        public ReferenceList(ImmutableProp prop) {
            super(prop);
            if (!prop.isReferenceList(TargetLevel.OBJECT)) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not reference list property"
                );
            }
        }
    }

    private static ImmutableProp nonNull(ImmutableProp prop) {
        if (prop.isNullable()) {
            throw new IllegalArgumentException(
                    "Cannot create non-null prop because \"" +
                            prop +
                            "\" is nullable"
            );
        }
        return prop;
    }

    private static ImmutableProp nullable(ImmutableProp prop) {
        if (!prop.isNullable()) {
            throw new IllegalArgumentException(
                    "Cannot create nullable prop because \"" +
                            prop +
                            "\" is not nullable"
            );
        }
        return prop;
    }
}
