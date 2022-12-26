package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.*;

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

    public static class Scalar<S, T> extends TypedPropImpl<S, T> implements TypedProp.Scalar<S, T> {

        private final boolean desc;

        private final boolean nullsFirst;

        private final boolean nullsLast;

        public Scalar(ImmutableProp prop) {
            this(prop, false, false, false);
        }

        private Scalar(
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
    }

    public static class ScalarList<S, T> extends TypedPropImpl<S, T> implements TypedProp.ScalarList<S, T> {

        public ScalarList(ImmutableProp prop) {
            super(prop);
            if (!prop.isScalarList()) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not scalar list property"
                );
            }
        }
    }

    public static class Reference<S, T> extends TypedPropImpl<S, T> implements TypedProp.Reference<S, T> {

        public Reference(ImmutableProp prop) {
            super(prop);
            if (!prop.isReference(TargetLevel.OBJECT)) {
                throw new IllegalArgumentException(
                        "\"" +
                                prop +
                                "\" is not reference property"
                );
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
}
