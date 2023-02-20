package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.impl.ExampleImpl;

public interface Example<E> {

    static <E> Example<E> of(E obj) {
        if (obj instanceof Input<?>) {
            throw new IllegalArgumentException(
                    "entity cannot be input, " +
                            "please call another overloaded function whose parameter is input"
            );
        }
        return new ExampleImpl<>(obj);
    }

    static <E> Example<E> of(Input<E> input) {
        return new ExampleImpl<>(input.toEntity());
    }

    @NewChain
    default Example<E> like(TypedProp.Scalar<E, String> prop) {
        return like(prop, LikeMode.ANYWHERE);
    }

    @NewChain
    Example<E> like(TypedProp.Scalar<E, String> prop, LikeMode likeMode);

    @NewChain
    Example<E> match(MatchMode mode);

    @NewChain
    Example<E> match(TypedProp<E, ?> prop, MatchMode matchMode);

    @NewChain
    Example<E> trim();

    @NewChain
    Example<E> trim(TypedProp.Scalar<E, String> prop);

    @NewChain
    Example<E> ignoreZero(TypedProp.Scalar<E, ? extends Number> prop);

    @NewChain
    default Example<E> ilike(TypedProp.Scalar<E, String> prop) {
        return ilike(prop, LikeMode.ANYWHERE);
    }

    @NewChain
    Example<E> ilike(TypedProp.Scalar<E, String> prop, LikeMode likeMode);

    enum MatchMode {
        NOT_EMPTY,
        NOT_NULL,
        NULLABLE
    }
}
