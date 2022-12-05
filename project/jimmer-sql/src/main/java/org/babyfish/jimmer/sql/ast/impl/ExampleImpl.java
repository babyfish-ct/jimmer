package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ExampleImpl<E> implements Example<E> {

    private final ImmutableSpi spi;

    private final ExampleImpl<E> prev;

    private final ImmutableProp prop;

    private final boolean likeInsensitive;

    private final LikeMode likeMode;

    public ExampleImpl(E obj) {
        if (!(obj instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("example must be immutable instance");
        }
        this.spi = (ImmutableSpi) obj;
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (spi.__isLoaded(prop.getId())) {
                if (!(prop.getStorage() instanceof ColumnDefinition)) {
                    throw new IllegalArgumentException(
                            "The prop \"" +
                                    prop +
                                    "\" of example object cannot be loaded, " +
                                    "example object does not accept properties not based on database columns"
                    );
                }
            }
        }
        this.prev = null;
        this.prop = null;
        this.likeInsensitive = false;
        this.likeMode = LikeMode.ANYWHERE;
    }

    private ExampleImpl(ExampleImpl<E> prev, TypedProp.Scalar<E, String> prop, boolean likeInsensitive, LikeMode likeMode) {
        this.spi = prev.spi;
        this.prev = prev;
        this.prop = Objects.requireNonNull(prop, "prop cannot be null").unwrap();
        this.likeInsensitive = likeInsensitive;
        this.likeMode = Objects.requireNonNull(likeMode, "likeMode cannot be null");
    }

    @NewChain
    @Override
    public ExampleImpl<E> like(TypedProp.Scalar<E, String> prop, LikeMode likeMode) {
        return new ExampleImpl<>(this, prop, false, likeMode);
    }

    @NewChain
    @Override
    public ExampleImpl<E> ilike(TypedProp.Scalar<E, String> prop, LikeMode likeMode) {
        return new ExampleImpl<>(this, prop, true, likeMode);
    }

    ImmutableType type() {
        return spi.__type();
    }

    void applyTo(MutableRootQueryImpl<?> query) {
        Map<ImmutableProp, ExampleImpl<?>> map = new HashMap<>();
        collect(map);
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (spi.__isLoaded(prop.getId())) {
                Expression<Object> expr = expressionOf(query.getTable(), prop);
                Object value = valueOf(spi, prop);
                if (value == null) {
                    query.where(expr.isNull());
                } else {
                    ExampleImpl<?> impl = map.get(prop);
                    if (impl == null) {
                        query.where(expr.eq(value));
                    } else if (impl.likeInsensitive) {
                        query.where(((StringExpression)(Expression<?>)expr).ilike((String)value, impl.likeMode));
                    } else {
                        query.where(((StringExpression)(Expression<?>)expr).like((String)value, impl.likeMode));
                    }
                }
            }
        }
    }

    void collect(Map<ImmutableProp, ExampleImpl<?>> map) {
        if (prop != null) {
            map.putIfAbsent(prop, this);
        }
        if (prev != null) {
            prev.collect(map);
        }
    }

    private static Expression<Object> expressionOf(Table<?> table, ImmutableProp prop) {
        if (prop.isReference(TargetLevel.ENTITY)) {
            Table<?> joinedExpr = table.join(prop.getName());
            return joinedExpr.get(prop.getTargetType().getIdProp().getName());
        }
        return table.get(prop.getName());
    }

    private static Object valueOf(ImmutableSpi spi, ImmutableProp prop) {
        Object value = spi.__get(prop.getId());
        if (value != null && prop.isReference(TargetLevel.ENTITY)) {
            return ((ImmutableSpi)value).__get(prop.getTargetType().getIdProp().getId());
        }
        return value;
    }
}
