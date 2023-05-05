package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ExampleImpl<E> implements Example<E> {

    private final static Predicate[] EMPTY_PREDICATES = new Predicate[0];

    private final ImmutableSpi spi;

    private final MatchMode matchMode;

    private final boolean trim;

    private final Map<ImmutableProp, PropData> propDataMap;

    public ExampleImpl(E obj) {
        if (!(obj instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("example must be immutable instance");
        }
        this.spi = (ImmutableSpi) obj;
        this.matchMode = MatchMode.NOT_EMPTY;
        this.trim = false;
        this.propDataMap = new HashMap<>();
    }

    private ExampleImpl(ExampleImpl<E> base, PropData data) {
        Map<ImmutableProp, PropData> newPropDataMap = new HashMap<>(base.propDataMap);
        newPropDataMap.put(data.prop, data);
        this.spi = base.spi;
        this.matchMode = base.matchMode;
        this.trim = base.trim;
        this.propDataMap = newPropDataMap;
    }

    private ExampleImpl(ExampleImpl<E> base, MatchMode matchMode) {
        this.spi = base.spi;
        this.matchMode = matchMode;
        this.trim = base.trim;
        this.propDataMap = base.propDataMap;
    }

    private ExampleImpl(ExampleImpl<E> base, boolean trim) {
        this.spi = base.spi;
        this.matchMode = base.matchMode;
        this.trim = trim;
        this.propDataMap = base.propDataMap;
    }

    @Override
    public Example<E> match(MatchMode mode) {
        if (this.matchMode == mode) {
            return this;
        }
        return new ExampleImpl<>(this, mode);
    }

    @Override
    public Example<E> match(TypedProp<E, ?> prop, MatchMode matchMode) {
        PropData data = propDataMap.get(prop.unwrap());
        if (data != null && data.matchMode == matchMode) {
            return this;
        }
        data = new PropData(
                prop.unwrap(),
                matchMode,
                data != null && data.trim,
                data != null && data.ignoreZero,
                data != null && data.likeInsensitive,
                data != null ? data.likeMode : LikeMode.EXACT
        );
        return new ExampleImpl<>(this, data);
    }

    @Override
    public Example<E> trim() {
        if (this.trim) {
            return this;
        }
        return new ExampleImpl<>(this, true);
    }

    @Override
    public Example<E> trim(TypedProp.Scalar<E, String> prop) {
        PropData data = propDataMap.get(prop.unwrap());
        if (data != null && data.trim) {
            return this;
        }
        data = new PropData(
                prop.unwrap(),
                data != null ? data.matchMode : null,
                true,
                data != null && data.ignoreZero,
                data != null && data.likeInsensitive,
                data != null ? data.likeMode : LikeMode.EXACT
        );
        return new ExampleImpl<>(this, data);
    }

    @Override
    public Example<E> ignoreZero(TypedProp.Scalar<E, ? extends Number> prop) {
        PropData data = propDataMap.get(prop.unwrap());
        if (data != null && data.ignoreZero) {
            return this;
        }
        data = new PropData(
                prop.unwrap(),
                data != null ? data.matchMode : null,
                data != null && data.trim,
                true,
                data != null && data.likeInsensitive,
                data != null ? data.likeMode : LikeMode.EXACT
        );
        return new ExampleImpl<>(this, data);
    }

    @NewChain
    @Override
    public ExampleImpl<E> like(TypedProp.Scalar<E, String> prop, LikeMode likeMode) {
        PropData data = propDataMap.get(prop.unwrap());
        if (data != null && !data.likeInsensitive && data.likeMode == likeMode) {
            return this;
        }
        data = new PropData(
                prop.unwrap(),
                data != null ? data.matchMode : null,
                data != null && data.trim,
                data != null && data.ignoreZero,
                false,
                likeMode
        );
        return new ExampleImpl<>(this, data);
    }

    @NewChain
    @Override
    public ExampleImpl<E> ilike(TypedProp.Scalar<E, String> prop, LikeMode likeMode) {
        PropData data = propDataMap.get(prop.unwrap());
        if (data != null && data.likeInsensitive && data.likeMode == likeMode) {
            return this;
        }
        data = new PropData(
                prop.unwrap(),
                data != null ? data.matchMode : null,
                data != null && data.trim,
                data != null && data.ignoreZero,
                true,
                likeMode
        );
        return new ExampleImpl<>(this, data);
    }

    ImmutableType type() {
        return spi.__type();
    }

    public Predicate toPredicate(Table<?> table) {
        List<Predicate> predicates = new ArrayList<>();
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (spi.__isLoaded(prop.getId()) && prop.isColumnDefinition()) {
                Object value = valueOf(spi, prop);
                Expression<Object> expr = expressionOf(table, prop, value == null ? JoinType.LEFT : JoinType.INNER);
                Predicate predicate = null;
                MatchMode matchMode = getMatchMode(prop);
                if (value == null) {
                    if (matchMode == MatchMode.NULLABLE) {
                        predicate = expr.isNull();
                    }
                } else if (value instanceof String) {
                    String str = (String) value;
                    if (isTrim(prop)) {
                        str = str.trim();
                    }
                    if (str.isEmpty() && matchMode == MatchMode.NOT_EMPTY) {
                        continue;
                    }
                    LikeMode likeMode = getLikeMode(prop);
                    if (isInsensitive(prop)) {
                        predicate = ((StringExpression)(Expression<?>)expr).ilike(str, likeMode);
                    } else if (likeMode != LikeMode.EXACT) {
                        predicate = ((StringExpression)(Expression<?>)expr).like(str, likeMode);
                    } else {
                        predicate = expr.eq(str);
                    }
                } else if (value instanceof Number) {
                    if (isZeroIgnored(prop) && ((Number)value).intValue() == 0) {
                        continue;
                    }
                    predicate = expr.eq(value);
                } else {
                    predicate = expr.eq(value);
                }
                predicates.add(predicate);
            }
        }
        return CompositePredicate.and(predicates.toArray(EMPTY_PREDICATES));
    }

    void applyTo(MutableRootQueryImpl<?> query) {
        query.where(toPredicate(query.getTable()));
    }

    private static Expression<Object> expressionOf(Table<?> table, ImmutableProp prop, JoinType joinType) {
        if (prop.isReference(TargetLevel.PERSISTENT)) {
            Table<?> joinedExpr = table.join(prop.getName(), joinType);
            return joinedExpr.get(prop.getTargetType().getIdProp().getName());
        }
        return table.get(prop.getName());
    }

    private static Object valueOf(ImmutableSpi spi, ImmutableProp prop) {
        Object value = spi.__get(prop.getId());
        if (value != null && prop.isReference(TargetLevel.PERSISTENT)) {
            return ((ImmutableSpi)value).__get(prop.getTargetType().getIdProp().getId());
        }
        return value;
    }

    private MatchMode getMatchMode(ImmutableProp prop) {
        PropData data = propDataMap.get(prop);
        return data != null && data.matchMode != null ? data.matchMode : matchMode;
    }

    private boolean isTrim(ImmutableProp prop) {
        PropData data = propDataMap.get(prop);
        return (data != null && data.trim) || trim;
    }

    private boolean isZeroIgnored(ImmutableProp prop) {
        PropData data = propDataMap.get(prop);
        return data != null && data.ignoreZero;
    }

    private boolean isInsensitive(ImmutableProp prop) {
        PropData data = propDataMap.get(prop);
        return data != null && data.likeInsensitive;
    }

    private LikeMode getLikeMode(ImmutableProp prop) {
        PropData data = propDataMap.get(prop);
        return data != null ? data.likeMode : LikeMode.EXACT;
    }

    private static class PropData {

        final ImmutableProp prop;

        @Nullable
        final MatchMode matchMode;

        final boolean trim;

        final boolean ignoreZero;

        final boolean likeInsensitive;

        final LikeMode likeMode;

        PropData(
                ImmutableProp prop,
                @Nullable MatchMode matchMode,
                boolean trim,
                boolean ignoreZero, boolean likeInsensitive,
                LikeMode likeMode
        ) {
            this.prop = prop;
            this.matchMode = matchMode;
            this.trim = trim;
            this.ignoreZero = ignoreZero;
            this.likeInsensitive = likeInsensitive;
            this.likeMode = likeMode;
        }
    }
}
