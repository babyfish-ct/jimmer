package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.meta.impl.TypedPropImpl;

import java.util.List;

/*
 * TypedProp<S, T> does not delete the wrapped ImmutableProp.
 * because the raw behavior of hashCode/equals of ImmutableProp is very important.
 */
public interface TypedProp<S, T> {

    ImmutableProp unwrap();

    default boolean match(ImmutableProp prop) {
        return prop == unwrap();
    }

    boolean isLoaded(Object immutable);

    interface NonNull<S, T> extends TypedProp<S, T> {}

    interface Nullable<S, T> extends TypedProp<S, T> {}

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
        interface NonNull<S, T> extends Scalar<S, T>, TypedProp.NonNull<S, T> {}
        interface Nullable<S, T> extends Scalar<S, T>, TypedProp.Nullable<S, T> {}
    }

    interface StringScalar<S> extends Scalar<S, String> {
        interface NonNull<S> extends StringScalar<S>, Scalar.NonNull<S, String> {}
        interface Nullable<S> extends StringScalar<S>, Scalar.Nullable<S, String> {}
    }

    interface NumericScalar<S, N extends Number & Comparable<N>> extends Scalar<S, N> {
        interface NonNull<S, N extends Number & Comparable<N>> extends NumericScalar<S, N>, Scalar.NonNull<S, N> {}
        interface Nullable<S, N extends Number & Comparable<N>> extends NumericScalar<S, N>, Scalar.Nullable<S, N> {}
    }

    interface ComparableScalar<S, T extends Comparable<?>> extends Scalar<S, T> {
        interface NonNull<S, T extends Comparable<?>> extends ComparableScalar<S, T>, Scalar.NonNull<S, T> {}
        interface Nullable<S, T extends Comparable<?>> extends ComparableScalar<S, T>, Scalar.Nullable<S, T> {}
    }

    interface ScalarList<S, T> extends TypedProp<S, T>, Multiple<S, T> {
        interface NonNull<S, T> extends ScalarList<S, T>, TypedProp.NonNull<S, T> {}
        interface Nullable<S, T> extends ScalarList<S, T>, TypedProp.Nullable<S, T> {}
    }

    interface Embedded<S, T> extends TypedProp<S, T>, Scalar<S, T> {
        interface NonNull<S, T> extends Embedded<S, T>, TypedProp.NonNull<S, T> {}
        interface Nullable<S, T> extends Embedded<S, T>, TypedProp.Nullable<S, T> {}
    }

    interface Association<S, T> extends TypedProp<S, T> {}

    interface Reference<S, T> extends Association<S, T>, Single<S, T> {
        interface NonNull<S, T> extends Reference<S, T>, TypedProp.NonNull<S, T> {}
        interface Nullable<S, T> extends Reference<S, T>, TypedProp.Nullable<S, T> {}
    }

    interface ReferenceList<S, T> extends Association<S, T>, Multiple<S, T>, NonNull<S, T> {}

    static <S, T> Scalar<S, T> scalar(ImmutableProp prop) {
        return TypedPropImpl.Scalar.of(prop);
    }

    static <S, T> ScalarList<S, T> scalarList(ImmutableProp prop) {
        return TypedPropImpl.ScalarList.of(prop);
    }

    static <S, T> Reference<S, T> reference(ImmutableProp prop) {
        return TypedPropImpl.Reference.of(prop);
    }

    static <S, T> ReferenceList<S, T> referenceList(ImmutableProp prop) {
        return new TypedPropImpl.ReferenceList<>(prop);
    }

    static <S> TypedProp.StringScalar.NonNull<S> nonNullString(ImmutableProp prop) {
        return new TypedPropImpl.StringScalar.NonNull<>(prop);
    }

    static <S> TypedProp.StringScalar.Nullable<S> nullableString(ImmutableProp prop) {
        return new TypedPropImpl.StringScalar.Nullable<>(prop);
    }

    static <S, N extends Number & Comparable<N>> NumericScalar.NonNull<S, N> nonNullNumeric(ImmutableProp prop) {
        return new TypedPropImpl.NumericScalar.NonNull<>(prop);
    }

    static <S, N extends Number & Comparable<N>> NumericScalar.Nullable<S, N> nullableNumeric(ImmutableProp prop) {
        return new TypedPropImpl.NumericScalar.Nullable<>(prop);
    }

    static <S, T extends Comparable<?>> TypedProp.ComparableScalar.NonNull<S, T> nonNullComparable(ImmutableProp prop) {
        return new TypedPropImpl.ComparableScalar.NonNull<>(prop);
    }

    static <S, T extends Comparable<?>> TypedProp.ComparableScalar.Nullable<S, T> nullableComparable(ImmutableProp prop) {
        return new TypedPropImpl.ComparableScalar.Nullable<>(prop);
    }

    static <S, T> TypedProp.Scalar.NonNull<S, T> nonNullScalar(ImmutableProp prop) {
        return new TypedPropImpl.Scalar.NonNull<>(prop);
    }

    static <S, T> TypedProp.Scalar.Nullable<S, T> nullableScalar(ImmutableProp prop) {
        return new TypedPropImpl.Scalar.Nullable<>(prop);
    }

    static <S, T> TypedProp.ScalarList.NonNull<S, T> nonNullScalarList(ImmutableProp prop) {
        return new TypedPropImpl.ScalarList.NonNull<>(prop);
    }

    static <S, T> TypedProp.ScalarList.Nullable<S, T> nullableScalarList(ImmutableProp prop) {
        return new TypedPropImpl.ScalarList.Nullable<>(prop);
    }

    static <S, T> TypedProp.Embedded.NonNull<S, T> nonNullEmbedded(ImmutableProp prop) {
        return new TypedPropImpl.Embedded.NonNull<>(prop);
    }

    static <S, T> TypedProp.Embedded.Nullable<S, T> nullableEmbedded(ImmutableProp prop) {
        return new TypedPropImpl.Embedded.Nullable<>(prop);
    }

    static <S, T> TypedProp.Reference.NonNull<S, T> nonNullReference(ImmutableProp prop) {
        return new TypedPropImpl.Reference.NonNull<>(prop);
    }

    static <S, T> TypedProp.Reference.Nullable<S, T> nullableReference(ImmutableProp prop) {
        return new TypedPropImpl.Reference.Nullable<>(prop);
    }
}
