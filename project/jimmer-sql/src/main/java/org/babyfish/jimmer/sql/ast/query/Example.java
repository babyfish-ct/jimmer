package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.impl.ExampleImpl;

public interface Example<E> {

    static <E> Example<E> of(E obj) {
        return new ExampleImpl<>(obj);
    }

    @NewChain
    default Example<E> like(TypedProp.Scalar<E, String> prop) {
        return like(prop, LikeMode.ANYWHERE);
    }

    @NewChain
    Example<E> like(TypedProp.Scalar<E, String> prop, LikeMode likeMode);

    @NewChain
    default Example<E> ilike(TypedProp.Scalar<E, String> prop) {
        return ilike(prop, LikeMode.ANYWHERE);
    }

    @NewChain
    Example<E> ilike(TypedProp.Scalar<E, String> prop, LikeMode likeMode);
}
