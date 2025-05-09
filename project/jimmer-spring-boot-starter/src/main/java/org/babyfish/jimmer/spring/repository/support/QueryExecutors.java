package org.babyfish.jimmer.spring.repository.support;

import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.spring.repository.DynamicQuery;
import org.babyfish.jimmer.spring.repository.SpringOrders;
import org.babyfish.jimmer.spring.repository.parser.*;
import org.babyfish.jimmer.spring.repository.parser.Predicate;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.mutation.Mutations;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification;
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecificationKt;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class QueryExecutors {

    private QueryExecutors() {}

    @SuppressWarnings("unchecked")
    public static Object execute(
            JSqlClientImplementor sqlClient,
            ImmutableType type,
            QueryMethod queryMethod,
            Pageable pageable,
            Sort sort,
            Specification<?> specification,
            Fetcher<?> fetcher,
            Class<?> viewType,
            Object[] args
    ) {
        Query queryData = queryMethod.getQuery();
        Method javaMethod = queryMethod.getJavaMethod();
        boolean isDynamicQuery = javaMethod.isAnnotationPresent(DynamicQuery.class) || javaMethod.getDeclaringClass().isAnnotationPresent(DynamicQuery.class);
        if (queryData.getAction() == Query.Action.DELETE) {
            int rowCount = Mutations.createDelete(sqlClient, type, (d, table) -> {
                d.where(astPredicate(table, queryData.getPredicate(), args, isDynamicQuery));
            }).execute();
            return javaMethod.getReturnType() == int.class ? rowCount : null;
        } else {
            ConfigurableRootQuery<?, Object> query = Queries
                    .createQuery(sqlClient, type, ExecutionPurpose.QUERY, FilterLevel.DEFAULT, (q, table) -> {
                        q.where(astPredicate(table, queryData.getPredicate(), args, isDynamicQuery));
                        if (specification != null) {
                            if (specification instanceof KSpecification<?>) {
                                JSpecification<?, Table<?>> spec =
                                        (JSpecification<?, Table<?>>)(JSpecification<?, ?>)
                                                KSpecificationKt.toJavaSpecification(((KSpecification<Object>)specification));
                                q.where(spec);
                            } else {
                                q.where((JSpecification<?, Table<?>>) specification);
                            }
                        }
                        for (Query.Order order : queryData.getOrders()) {
                            q.orderBy(
                                    order.getOrderMode() == OrderMode.DESC ?
                                            ((Expression<?>) astSelection(table, order.getPath(), true)).desc() :
                                            ((Expression<?>) astSelection(table, order.getPath(), true)).asc()
                            );
                        }
                        Sort finalSort = pageable != null ? pageable.getSort() : sort;
                        if (finalSort != null) {
                            q.orderBy(SpringOrders.toOrders(table, finalSort));
                        }
                        if (fetcher != null) {
                            return q.select(((Table<Object>)table).fetch((Fetcher<Object>) fetcher));
                        }
                        if (viewType != null) {
                            return (ConfigurableRootQuery<Table<?>, Object>) (ConfigurableRootQuery<?, ?>)q.select(
                                    ((Table<Object>)table).fetch((Class<View<Object>>)viewType)
                            );
                        }
                        if (queryData.getSelectedPath() != null) {
                            return q.select((Expression<Object>) astSelection(table, queryData.getSelectedPath(), false));
                        }
                        if (queryData.getAction() == Query.Action.COUNT) {
                            return q.select((Expression<Object>)(Expression<?>)table.count());
                        }
                        if (queryData.getAction() == Query.Action.EXISTS) {
                            return q.select(table.get(table.getImmutableType().getIdProp()));
                        }
                        return q.select((Table<Object>)table);
                    });
            Class<?> returnType = javaMethod.getReturnType();
            switch (queryData.getAction()) {
                case FIND:
                    if (returnType == Page.class) {
                        return query.fetchPage(pageable.getPageNumber(), pageable.getPageSize(), SpringPageFactory.getInstance());
                    }
                    if (returnType == org.babyfish.jimmer.Page.class) {
                        return query.fetchPage(pageable.getPageNumber(), pageable.getPageSize());
                    }
                    if (queryData.getLimit() != Integer.MAX_VALUE) {
                        query = query.limit(queryData.getLimit(), 0);
                    }
                    if (queryData.isDistinct()) {
                        query = query.distinct();
                    }
                    if (Iterable.class.isAssignableFrom(returnType)) {
                        return query.execute();
                    }
                    Object entity = query.fetchOneOrNull();
                    return returnType == Optional.class ? Optional.ofNullable(entity) : entity;
                case COUNT:
                    long rowCount = (Long)query.execute().get(0);
                    if (returnType == int.class) {
                        return (int)rowCount;
                    }
                    return rowCount;
                case EXISTS:
                    return query.limit(1, 0).fetchOneOrNull() != null;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static org.babyfish.jimmer.sql.ast.Predicate astPredicate(
            Table<?> table,
            Predicate predicate,
            Object[] args,
            boolean isDynamicQuery
    ) {
        if (predicate == null) {
            return null;
        }
        if (predicate instanceof PropPredicate) {
            PropPredicate propPredicate = (PropPredicate) predicate;
            Selection<?> astSelection;
            switch (propPredicate.getOp()) {
                case NULL:
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
                    return c == null && isDynamicQuery ? null : ((Expression<Object>)astSelection).in(c);
                }
                case NOT_IN: {
                    Collection<Object> c = (Collection<Object>) args[propPredicate.getLogicParamIndex()];
                    return c == null && isDynamicQuery ? null : ((Expression<Object>)astSelection).notIn(c);
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
                    return (pattern == null || pattern.isEmpty()) && isDynamicQuery ?
                            null :
                            propPredicate.isInsensitive() ?
                                    ((StringExpression) astSelection).ilike(pattern, propPredicate.getLikeMode()) :
                                    ((StringExpression) astSelection).like(pattern, propPredicate.getLikeMode());
                }
                case NOT_LIKE: {
                    String pattern = (String) args[propPredicate.getLogicParamIndex()];
                    return (pattern == null || pattern.isEmpty()) && isDynamicQuery ?
                            null :
                            org.babyfish.jimmer.sql.ast.Predicate.not(
                                    propPredicate.isInsensitive() ?
                                            ((StringExpression) astSelection).ilike(pattern, propPredicate.getLikeMode()) :
                                            ((StringExpression) astSelection).like(pattern, propPredicate.getLikeMode())
                            );
                }
                case EQ: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Object value = insensitive(propPredicate.isInsensitive(), args[propPredicate.getLogicParamIndex()]);
                    return value == null && isDynamicQuery ? null : ((Expression<Object>) astSelection).eq(value);
                }
                case NE: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Object value = insensitive(propPredicate.isInsensitive(), args[propPredicate.getLogicParamIndex()]);
                    return value == null && isDynamicQuery ? null : ((Expression<Object>) astSelection).ne(value);
                }
                case LT: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null && isDynamicQuery ? null : ((ComparableExpression) astSelection).lt(value);
                }
                case LE: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null && isDynamicQuery ? null : ((ComparableExpression) astSelection).le(value);
                }
                case GT: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null && isDynamicQuery ? null : ((ComparableExpression) astSelection).gt(value);
                }
                case GE: {
                    astSelection = insensitive(propPredicate.isInsensitive(), astSelection);
                    Comparable value = (Comparable) insensitive(
                            propPredicate.isInsensitive(),
                            args[propPredicate.getLogicParamIndex()]
                    );
                    return value == null && isDynamicQuery ? null : ((ComparableExpression) astSelection).ge(value);
                }
            }
        }
        if (predicate instanceof AndPredicate) {
            List<Predicate> subPredicates = ((AndPredicate)predicate).getPredicates();
            org.babyfish.jimmer.sql.ast.Predicate[] subAstPredicates =
                    new org.babyfish.jimmer.sql.ast.Predicate[subPredicates.size()];
            int index = 0;
            for (Predicate subPredicate : subPredicates) {
                subAstPredicates[index++] = astPredicate(table, subPredicate, args, isDynamicQuery);
            }
            return org.babyfish.jimmer.sql.ast.Predicate.and(subAstPredicates);
        }
        if (predicate instanceof OrPredicate) {
            List<Predicate> subPredicates = ((OrPredicate)predicate).getPredicates();
            org.babyfish.jimmer.sql.ast.Predicate[] subAstPredicates =
                    new org.babyfish.jimmer.sql.ast.Predicate[subPredicates.size()];
            int index = 0;
            for (Predicate subPredicate : subPredicates) {
                subAstPredicates[index++] = astPredicate(table, subPredicate, args, isDynamicQuery);
            }
            return org.babyfish.jimmer.sql.ast.Predicate.or(subAstPredicates);
        }
        throw new AssertionError("Internal bug, unexpected prop predicate " + predicate);
    }

    private static Selection<?> astSelection(Table<?> table, Path path, boolean outerJoin) {
        PropExpression<?> propExpr = null;
        for (ImmutableProp prop : path.getProps()) {
            if (prop.isAssociation(TargetLevel.PERSISTENT)) {
                table = table.join(prop, outerJoin ? JoinType.LEFT : JoinType.INNER);
            } else if (propExpr instanceof PropExpression.Embedded<?>) {
                propExpr = ((PropExpression.Embedded<?>) propExpr).get(prop);
            } else {
                propExpr = table.get(prop);
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
