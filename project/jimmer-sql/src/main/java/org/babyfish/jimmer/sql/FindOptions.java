package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.Collection;

public interface FindOptions<T> {

    static <T> FindOptions<T> of(Class<T> type) {
        return null;
    }

    static <T> FindOptions<T> of(Fetcher<T> fetcher) {
        return null;
    }

    static <T> ByExample<T> of(Class<T> type, T example) {
        return null;
    }

    static <T> ByExample<T> of(Fetcher<T> type, T example) {
        return null;
    }

    <X> FindOptions<T> eq(TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> ne(TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> lt(TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> le(TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> gt(TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> ge(TypedProp.Scalar<T, ?> prop, X value);

    FindOptions<T> idEq(TypedProp.Reference<T, ?> prop, Object id);

    FindOptions<T> idIn(TypedProp.Reference<T, ?> prop, Collection<Object> ids);

    FindOptions<T> idNotIn(TypedProp.Reference<T, ?> prop, Collection<Object> ids);

    default FindOptions<T> like(TypedProp.Scalar<T, String> prop, String pattern) {
        return like(prop, pattern, LikeMode.ANYWHERE);
    }

    FindOptions<T> like(TypedProp.Scalar<T, String> prop, String pattern, LikeMode mode);

    default FindOptions<T> ilike(TypedProp.Scalar<T, String> prop, String pattern) {
        return ilike(prop, pattern, LikeMode.ANYWHERE);
    }

    FindOptions<T> ilike(TypedProp.Scalar<T, String> prop, String pattern, LikeMode mode);

    <X> FindOptions<T> between(TypedProp.Scalar<T, ?> prop, X min, X max);

    <X> FindOptions<T> eqIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> neIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> ltIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> leIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> gtIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> geIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

    <X> FindOptions<T> betweenIf(boolean condition, TypedProp.Scalar<T, ?> prop, X min, X max);

    FindOptions<T> idEqIf(boolean condition, TypedProp.Reference<T, ?> prop, Object id);

    FindOptions<T> idInIf(boolean condition, TypedProp.Reference<T, ?> prop, Collection<Object> ids);

    FindOptions<T> idNotInIf(boolean condition, TypedProp.Reference<T, ?> prop, Collection<Object> ids);

    default FindOptions<T> likeIf(boolean condition, TypedProp.Scalar<T, String> prop, String pattern) {
        return like(prop, pattern, LikeMode.ANYWHERE);
    }

    FindOptions<T> likeIf(boolean condition, TypedProp.Scalar<T, String> prop, String pattern, LikeMode mode);

    default FindOptions<T> ilikeIf(boolean condition, TypedProp.Scalar<T, String> prop, String pattern) {
        return ilikeIf(condition, prop, pattern, LikeMode.ANYWHERE);
    }

    FindOptions<T> ilikeIf(boolean condition, TypedProp.Scalar<T, String> prop, String pattern, LikeMode mode);

    FindOptions<T> asc(TypedProp.Scalar<T, ?> prop);

    FindOptions<T> desc(TypedProp.Scalar<T, ?> prop);

    interface ByExample<T> extends FindOptions<T> {

        @Override
        <X> ByExample<T> eq(TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> ne(TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> lt(TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> le(TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> gt(TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> ge(TypedProp.Scalar<T, ?> prop, X value);

        @Override
        ByExample<T> idEq(TypedProp.Reference<T, ?> prop, Object id);

        @Override
        ByExample<T> idIn(TypedProp.Reference<T, ?> prop, Collection<Object> ids);

        @Override
        ByExample<T> idNotIn(TypedProp.Reference<T, ?> prop, Collection<Object> ids);

        @Override
        default ByExample<T> like(TypedProp.Scalar<T, String> prop, String pattern) {
            return like(prop, pattern, LikeMode.ANYWHERE);
        }

        @Override
        ByExample<T> like(TypedProp.Scalar<T, String> prop, String pattern, LikeMode mode);

        @Override
        default ByExample<T> ilike(TypedProp.Scalar<T, String> prop, String pattern) {
            return ilike(prop, pattern, LikeMode.ANYWHERE);
        }

        @Override
        ByExample<T> ilike(TypedProp.Scalar<T, String> prop, String pattern, LikeMode mode);

        @Override
        <X> ByExample<T> between(TypedProp.Scalar<T, ?> prop, X min, X max);

        @Override
        <X> ByExample<T> eqIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> neIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> ltIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> leIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> gtIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> geIf(boolean condition, TypedProp.Scalar<T, ?> prop, X value);

        @Override
        <X> ByExample<T> betweenIf(boolean condition, TypedProp.Scalar<T, ?> prop, X min, X max);

        @Override
        ByExample<T> idEqIf(boolean condition, TypedProp.Reference<T, ?> prop, Object id);

        @Override
        ByExample<T> idInIf(boolean condition, TypedProp.Reference<T, ?> prop, Collection<Object> ids);

        @Override
        ByExample<T> idNotInIf(boolean condition, TypedProp.Reference<T, ?> prop, Collection<Object> ids);

        @Override
        default ByExample<T> likeIf(boolean condition, TypedProp.Scalar<T, String> prop, String pattern) {
            return likeIf(condition, prop, pattern, LikeMode.ANYWHERE);
        }

        @Override
        ByExample<T> likeIf(boolean condition, TypedProp.Scalar<T, String> prop, String pattern, LikeMode mode);

        @Override
        default ByExample<T> ilikeIf(boolean condition, TypedProp.Scalar<T, String> prop, String pattern) {
            return ilikeIf(condition, prop, pattern, LikeMode.ANYWHERE);
        }

        @Override
        ByExample<T> ilikeIf(boolean condition, TypedProp.Scalar<T, String> prop, String pattern, LikeMode mode);

        @Override
        ByExample<T> asc(TypedProp.Scalar<T, ?> prop);

        @Override
        ByExample<T> desc(TypedProp.Scalar<T, ?> prop);

        ByExample<T> matchExample(TypedProp.Scalar<T, String> prop, LikeMode likeMode);
    }
}
