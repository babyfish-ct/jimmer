package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.spring.repository.Sorts;
import org.babyfish.jimmer.spring.repository.parser.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.impl.mutation.Mutations;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class JavaExecutors {

    private JavaExecutors() {}

    @SuppressWarnings("unchecked")
    public static Object execute(
            JSqlClient sqlClient,
            ImmutableType type,
            QueryMethod queryMethod,
            Pageable pageable,
            Sort sort,
            Fetcher<?> fetcher,
            Object[] args
    ) {
        Query queryData = queryMethod.getQuery();
        if (queryData.getAction() == Query.Action.DELETE) {
            int rowCount = Mutations.createDelete(sqlClient, type, (d, table) -> {
                d.where(astPredicate(table, queryData.getPredicate(), args));
            }).execute();
            return queryMethod.getJavaMethod().getReturnType() == int.class ? rowCount : null;
        } else {
            ConfigurableRootQuery<?, Object> query = Queries
                    .createQuery(sqlClient, type, ExecutionPurpose.QUERY, false, (q, table) -> {
                        q.where(astPredicate(table, queryData.getPredicate(), args));
                        for (Query.Order order : queryData.getOrders()) {
                            q.orderBy(
                                    order.getOrderMode() == OrderMode.DESC ?
                                            astExpression(table, order.getPath(), true).desc() :
                                            astExpression(table, order.getPath(), true).asc()
                            );
                        }
                        Sort finalSort = pageable != null ? pageable.getSort() : sort;
                        if (finalSort != null) {
                            q.orderBy(Sorts.toOrders(table, finalSort));
                        }
                        if (fetcher != null) {
                            return q.select(((Table<Object>)table).fetch((Fetcher<Object>) fetcher));
                        }
                        if (queryData.getSelectedPath() != null) {
                            return q.select((Expression<Object>)astExpression(table, queryData.getSelectedPath(), false));
                        }
                        if (queryData.getAction() == Query.Action.COUNT) {
                            return q.select((Expression<Object>)(Expression<?>)table.count());
                        }
                        if (queryData.getAction() == Query.Action.EXISTS) {
                            return q.select(table.<Expression<Object>>get(table.getImmutableType().getIdProp().getName()));
                        }
                        return q.select((Table<Object>)table);
                    });
            Class<?> returnType = queryMethod.getJavaMethod().getReturnType();
            switch (queryData.getAction()) {
                case FIND:
                    if (queryData.getLimit() != Integer.MAX_VALUE) {
                        query = query.limit(queryData.getLimit(), 0);
                    }
                    if (queryData.isDistinct()) {
                        query = query.distinct();
                    }
                    if (returnType == Page.class) {
                        if (pageable != null) {
                            int rowCount = query.count();
                            List<Object> entities = query.limit(pageable.getPageSize(), (int) pageable.getOffset()).execute();
                            return new PageImpl<>(entities, pageable, rowCount);
                        }
                        return new PageImpl<>(query.execute());
                    }
                    if (Iterable.class.isAssignableFrom(returnType)) {
                        return query.execute();
                    }
                    Object entity = query.fetchOne();
                    return returnType == Optional.class ? Optional.ofNullable(entity) : entity;
                case COUNT:
                    long rowCount = (Long)query.fetchOne();
                    return returnType == int.class ? (int)rowCount : rowCount;
                case EXISTS:
                    return query.limit(1, 0).fetchOne() != null;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static org.babyfish.jimmer.sql.ast.Predicate astPredicate(
            Table<?> table,
            Predicate predicate,
            Object[] args
    ) {
        if (predicate == null) {
            return null;
        }
        if (predicate instanceof PropPredicate) {
            PropPredicate propPredicate = (PropPredicate) predicate;
            Expression<?> astExpression;
            switch (propPredicate.getOp()) {
                case NOT_IN:
                case NOT_NULL:
                    astExpression = astExpression(table, propPredicate.getPath(), true);
                    break;
                default:
                    astExpression = astExpression(table, propPredicate.getPath(), false);
                    break;
            }
            switch (propPredicate.getOp()) {
                case TRUE:
                    return ((Expression<Boolean>)astExpression).eq(true);
                case FALSE:
                    return ((Expression<Boolean>)astExpression).eq(false);
                case NULL:
                    return astExpression(table, propPredicate.getPath(), true).isNull();
                case NOT_NULL:
                    return astExpression(table, propPredicate.getPath(), true).isNotNull();
                case IN: {
                    Collection<Object> c = (Collection<Object>) args[propPredicate.getLogicParamIndex()];
                    return c == null ? null : ((Expression<Object>)astExpression).in(c);
                }
                case NOT_IN: {
                    Collection<Object> c = (Collection<Object>) args[propPredicate.getLogicParamIndex()];
                    return c == null ? null : ((Expression<Object>)astExpression).notIn(c);
                }
                case BETWEEN: {
                    astExpression = insensitive(propPredicate.isInsensitive(), astExpression);
                    Comparable min = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    Comparable max = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex2()]
                    );
                    if (min != null && max != null) {
                        return ((ComparableExpression)astExpression).between(min, max);
                    }
                    if (min != null) {
                        return ((ComparableExpression)astExpression).ge(min);
                    }
                    if (max != null) {
                        return ((ComparableExpression)astExpression).le(max);
                    }
                    return null;
                }
                case NOT_BETWEEN: {
                    astExpression = insensitive(propPredicate.isInsensitive(), astExpression);
                    Comparable min = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    Comparable max = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex2()]
                    );
                    if (min != null && max != null) {
                        return ((ComparableExpression)astExpression).notBetween(min, max);
                    }
                    if (min != null) {
                        return ((ComparableExpression)astExpression).lt(min);
                    }
                    if (max != null) {
                        return ((ComparableExpression)astExpression).gt(max);
                    }
                    return null;
                }
                case LIKE: {
                    String pattern = (String) args[propPredicate.getLogicParamIndex()];
                    return pattern == null || pattern.isEmpty() ?
                            null :
                            propPredicate.isInsensitive() ?
                                    ((StringExpression) astExpression).ilike(pattern, propPredicate.getLikeMode()) :
                                    ((StringExpression) astExpression).like(pattern, propPredicate.getLikeMode());
                }
                case NOT_LIKE: {
                    String pattern = (String) args[propPredicate.getLogicParamIndex()];
                    return pattern == null || pattern.isEmpty() ?
                            null :
                            propPredicate.isInsensitive() ?
                                    ((StringExpression) astExpression).ilike(pattern, propPredicate.getLikeMode()).not() :
                                    ((StringExpression) astExpression).like(pattern, propPredicate.getLikeMode()).not();
                }
                case EQ: {
                    astExpression = insensitive(propPredicate.isInsensitive(), astExpression);
                    Object value = insensitive(propPredicate.isInsensitive(), args[propPredicate.getLogicParamIndex()]);
                    return value == null ? null : ((Expression<Object>) astExpression).eq(value);
                }
                case NE: {
                    astExpression = insensitive(propPredicate.isInsensitive(), astExpression);
                    Object value = insensitive(propPredicate.isInsensitive(), args[propPredicate.getLogicParamIndex()]);
                    return value == null ? null : ((Expression<Object>) astExpression).ne(value);
                }
                case LT: {
                    astExpression = insensitive(propPredicate.isInsensitive(), astExpression);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null ? null : ((ComparableExpression) astExpression).lt(value);
                }
                case LE: {
                    astExpression = insensitive(propPredicate.isInsensitive(), astExpression);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null ? null : ((ComparableExpression) astExpression).le(value);
                }
                case GT: {
                    astExpression = insensitive(propPredicate.isInsensitive(), astExpression);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null ? null : ((ComparableExpression) astExpression).gt(value);
                }
                case GE: {
                    astExpression = insensitive(propPredicate.isInsensitive(), astExpression);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null ? null : ((ComparableExpression) astExpression).ge(value);
                }
            }
        }
        if (predicate instanceof AndPredicate) {
            List<Predicate> subPredicates = ((AndPredicate)predicate).getPredicates();
            org.babyfish.jimmer.sql.ast.Predicate[] subAstPredicates =
                    new org.babyfish.jimmer.sql.ast.Predicate[subPredicates.size()];
            int index = 0;
            for (Predicate subPredicate : subPredicates) {
                subAstPredicates[index++] = astPredicate(table, subPredicate, args);
            }
            return org.babyfish.jimmer.sql.ast.Predicate.and(subAstPredicates);
        }
        if (predicate instanceof OrPredicate) {
            List<Predicate> subPredicates = ((OrPredicate)predicate).getPredicates();
            org.babyfish.jimmer.sql.ast.Predicate[] subAstPredicates =
                    new org.babyfish.jimmer.sql.ast.Predicate[subPredicates.size()];
            int index = 0;
            for (Predicate subPredicate : subPredicates) {
                subAstPredicates[index++] = astPredicate(table, subPredicate, args);
            }
            return org.babyfish.jimmer.sql.ast.Predicate.or(subAstPredicates);
        }
        throw new AssertionError("Internal bug, unexpected prop predicate " + predicate);
    }

    private static Expression<?> astExpression(Table<?> table, Path path, boolean outerJoin) {
        PropExpression<?> propExpr = null;
        for (ImmutableProp prop : path.getProps()) {
            if (prop.isAssociation(TargetLevel.ENTITY)) {
                table = table.join(prop.getName(), outerJoin ? JoinType.LEFT : JoinType.INNER);
            } else if (propExpr instanceof PropExpression.Embedded<?>) {
                propExpr = ((PropExpression.Embedded<?>) propExpr).get(prop.getName());
            } else {
                propExpr = table.get(prop.getName());
            }
        }
        return propExpr;
    }

    private static Expression<?> insensitive(boolean apply, Expression<?> astExpression) {
        if (apply) {
            return ((StringExpression) astExpression).lower();
        }
        return astExpression;
    }

    private static Object insensitive(boolean apply, Object arg) {
        if (apply && arg != null) {
            return ((String) arg).toLowerCase();
        }
        return arg;
    }
}
