package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.query.OrderMode;

import java.util.List;

public class Query {

    private final Action action;

    private final int limit;

    private final boolean distinct;

    private final Path selectedPath;

    private final Predicate predicate;

    private final List<Order> orders;

    public Query(Action action, int limit, boolean distinct, Path selectedPath, Predicate predicate, List<Order> orders) {
        this.action = action;
        this.limit = limit;
        this.distinct = distinct;
        this.selectedPath = selectedPath;
        this.predicate = predicate;
        this.orders = orders;
    }

    public Query(Query base, Predicate predicate) {
        this.action = base.action;
        this.limit = base.limit;
        this.distinct = base.distinct;
        this.selectedPath = base.selectedPath;
        this.predicate = predicate;
        this.orders = base.orders;
    }

    public Action getAction() {
        return action;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public Path getSelectedPath() {
        return selectedPath;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public List<Order> getOrders() {
        return orders;
    }

    @Override
    public String toString() {
        return "Query{" +
                "action=" + action +
                ", limit=" + limit +
                ", distinct=" + distinct +
                ", selectedPath=" + selectedPath +
                ", predicate=" + predicate +
                ", orders=" + orders +
                '}';
    }

    public static Query of(Context ctx, Source source, ImmutableType type) {
        return new QueryParser(ctx, type).parse(source);
    }

    public enum Action {
        FIND,
        COUNT,
        EXISTS,
        DELETE
    }

    public static class Order {

        private final Path path;

        private final OrderMode orderMode;

        public Order(Path path, OrderMode orderMode) {
            this.path = path;
            this.orderMode = orderMode;
        }

        public Path getPath() {
            return path;
        }

        public OrderMode getOrderMode() {
            return orderMode;
        }

        @Override
        public String toString() {
            return "Order{" +
                    "path=" + path +
                    ", orderMode=" + orderMode +
                    '}';
        }
    }
}
