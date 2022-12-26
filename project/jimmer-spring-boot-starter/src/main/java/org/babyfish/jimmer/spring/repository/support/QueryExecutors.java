package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.spring.repository.Sorts;
import org.babyfish.jimmer.spring.repository.parser.*;
import org.babyfish.jimmer.spring.repository.parser.Predicate;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.*;
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

public class QueryExecutors {

    private QueryExecutors() {}

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
                                            ((Expression<?>) astSelection(table, order.getPath(), true)).desc() :
                                            ((Expression<?>) astSelection(table, order.getPath(), true)).asc()
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
                            return q.select((Expression<Object>) astSelection(table, queryData.getSelectedPath(), false));
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
            Selection<?> astSelection;
            switch (propPredicate.getOp()) {
                case NOT_IN:
                case NOT_NULL:
                    astSelection = astSelection(table, propPredicate.getPath(), true);
                    break;
                default:
                    astSelection = astSelection(table, propPredicate.getPath(), false);
                    break;
            }
            switch (propPredicate.getOp()) {
                case TRUE:
                    return ((Expression<Boolean>)astSelection).eq(true);
                case FALSE:
                    return ((Expression<Boolean>)astSelection).eq(false);
                case NULL:
                    return astSelection instanceof Expression<?> ?
                            ((Expression<?>)astSelection).isNull() :
                            ((Table<?>)astSelection).isNull();
                case NOT_NULL:
                    return astSelection instanceof Expression<?> ?
                            ((Expression<?>)astSelection).isNotNull() :
                            ((Table<?>)astSelection).isNotNull();
                case IN: {
                    Collection<Object> c = (Collection<Object>) args[propPredicate.getLogicParamIndex()];
                    return c == null ? null : ((Expression<Object>)astSelection).in(c);
                }
                case NOT_IN: {
                    Collection<Object> c = (Collection<Object>) args[propPredicate.getLogicParamIndex()];
                    return c == null ? null : ((Expression<Object>)astSelection).notIn(c);
                }
                case BETWEEN: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable min = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    Comparable max = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex2()]
                    );
                    if (min != null && max != null) {
                        return ((ComparableExpression)astSelection).between(min, max);
                    }
                    if (min != null) {
                        return ((ComparableExpression)astSelection).ge(min);
                    }
                    if (max != null) {
                        return ((ComparableExpression)astSelection).le(max);
                    }
                    return null;
                }
                case NOT_BETWEEN: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable min = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    Comparable max = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex2()]
                    );
                    if (min != null && max != null) {
                        return ((ComparableExpression)astSelection).notBetween(min, max);
                    }
                    if (min != null) {
                        return ((ComparableExpression)astSelection).lt(min);
                    }
                    if (max != null) {
                        return ((ComparableExpression)astSelection).gt(max);
                    }
                    return null;
                }
                case LIKE: {
                    String pattern = (String) args[propPredicate.getLogicParamIndex()];
                    return pattern == null || pattern.isEmpty() ?
                            null :
                            propPredicate.isInsensitive() ?
                                    ((StringExpression) astSelection).ilike(pattern, propPredicate.getLikeMode()) :
                                    ((StringExpression) astSelection).like(pattern, propPredicate.getLikeMode());
                }
                case NOT_LIKE: {
                    String pattern = (String) args[propPredicate.getLogicParamIndex()];
                    return pattern == null || pattern.isEmpty() ?
                            null :
                            propPredicate.isInsensitive() ?
                                    ((StringExpression) astSelection).ilike(pattern, propPredicate.getLikeMode()).not() :
                                    ((StringExpression) astSelection).like(pattern, propPredicate.getLikeMode()).not();
                }
                case EQ: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Object value = insensitive(propPredicate.isInsensitive(), args[propPredicate.getLogicParamIndex()]);
                    return value == null ? null : ((Expression<Object>) astSelection).eq(value);
                }
                case NE: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Object value = insensitive(propPredicate.isInsensitive(), args[propPredicate.getLogicParamIndex()]);
                    return value == null ? null : ((Expression<Object>) astSelection).ne(value);
                }
                case LT: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null ? null : ((ComparableExpression) astSelection).lt(value);
                }
                case LE: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null ? null : ((ComparableExpression) astSelection).le(value);
                }
                case GT: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null ? null : ((ComparableExpression) astSelection).gt(value);
                }
                case GE: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null ? null : ((ComparableExpression) astSelection).ge(value);
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

    private static Selection<?> astSelection(Table<?> table, Path path, boolean outerJoin) {
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
        return propExpr != null ? propExpr : table;
    }

    private static Expression<?> insensitive(boolean apply, Selection<?> astExpression) {
        if (apply) {
            return ((StringExpression) astExpression).lower();
        }
        return (Expression<?>) astExpression;
    }

    private static Object insensitive(boolean apply, Object arg) {
        if (apply && arg != null) {
            return ((String) arg).toLowerCase();
        }
        return arg;
    }
}
