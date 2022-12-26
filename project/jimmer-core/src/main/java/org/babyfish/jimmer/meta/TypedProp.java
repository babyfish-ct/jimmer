package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.meta.impl.TypedPropImpl;

/*
 * TypedProp<S, T> does not delete the wrapped ImmutableProp.
 * because the raw behavior of hashCode/equals of ImmutableProp is very important.
 */
public interface TypedProp<S, T> {

    ImmutableProp unwrap();

    interface Single<S, T> extends TypedProp<S, T> {}

    interface Multiple<S, T> extends TypedProp<S, T> {}

    interface Scalar<S, T> extends TypedProp<S, T>, Single<S, T> {
        Scalar<S, T> asc();
        Scalar<S, T> desc();
        Scalar<S, T> nullsFirst();
        Scalar<S, T> nullsLast();
        boolean isDesc();
        boolean isNullsFirst();
        boolean isNullsLast();
    }

    interface ScalarList<S, T> extends TypedProp<S, T>, Multiple<S, T> {}

    interface Association<S, T> extends TypedProp<S, T> {}

    interface Reference<S, T> extends Association<S, T>, Single<S, T> {}

    interface ReferenceList<S, T> extends Association<S, T>, Multiple<S, T> {}

    static <S, T> Scalar<S, T> scalar(ImmutableProp prop) {
        return new TypedPropImpl.Scalar<>(prop);
    }

    static <S, T> ScalarList<S, T> scalarList(ImmutableProp prop) {
        return new TypedPropImpl.ScalarList<>(prop);
    }

    static <S, T> Reference<S, T> reference(ImmutableProp prop) {
        return new TypedPropImpl.Reference<>(prop);
    }

    static <S, T> ReferenceList<S, T> referenceList(ImmutableProp prop) {
        return new TypedPropImpl.ReferenceList<>(prop);
    }
}
