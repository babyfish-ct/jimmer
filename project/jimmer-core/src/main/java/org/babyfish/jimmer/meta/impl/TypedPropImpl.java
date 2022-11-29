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

        public Scalar(ImmutableProp prop) {
            super(prop);
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
            return this;
        }

        @Override
        public TypedProp.Scalar<S, T> desc() {
            return new Desc<S, T>(prop);
        }

        static class Desc<S, T> extends TypedPropImpl.Scalar<S, T> implements TypedProp.Scalar.Desc<S,  T> {

            public Desc(ImmutableProp prop) {
                super(prop);
            }

            @Override
            public TypedProp.Scalar<S, T> asc() {
                return new TypedPropImpl.Scalar<>(prop);
            }

            @Override
            public TypedProp.Scalar<S, T> desc() {
                return this;
            }
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
