package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.Props;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Order {

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[,;]");

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final Expression<?> expression;

    private final OrderMode orderMode;

    private final NullOrderMode nullOrderMode;

    public Order(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode) {
        this.expression = Objects.requireNonNull(expression);
        this.orderMode = Objects.requireNonNull(orderMode);
        this.nullOrderMode = Objects.requireNonNull(nullOrderMode);
    }

    public Expression<?> getExpression() {
        return expression;
    }

    public OrderMode getOrderMode() {
        return orderMode;
    }

    public NullOrderMode getNullOrderMode() {
        return nullOrderMode;
    }

    @NewChain
    public Order nullsFirst() {
        return new Order(expression, orderMode, NullOrderMode.NULLS_FIRST);
    }

    @NewChain
    public Order nullsLast() {
        return new Order(expression, orderMode, NullOrderMode.NULLS_LAST);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return expression.equals(order.expression) && orderMode == order.orderMode && nullOrderMode == order.nullOrderMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, orderMode, nullOrderMode);
    }

    @Override
    public String toString() {
        return "Order{" +
                "expression=" + expression +
                ", orderMode=" + orderMode +
                ", nullOrderMode=" + nullOrderMode +
                '}';
    }

    /**
     * Parse dynamic code to jimmer order list,
     *
     * For example:
     *
     * <pre><code>
     * q.makeOrders(
     *     Order.orders(
     *         table,
     *         "name asc, parent.name desc nulls last"
     *         "edition desc",
     *         "region.segment.size; region.segment.cost desc"
     *     )
     * )
     * </code></pre>
     */
    public static List<Order> makeOrders(Props table, String ... codes) {
        CustomOrderCreator<Order> creator = (path, orderMode, nullOrderMode) ->
                new Order(
                        orderedExpression(table, path),
                        orderMode,
                        nullOrderMode
                );
        return makeCustomOrders(creator, codes);
    }

    /**
     * Parse dynamic code to order list of third-party library, such as `Sort.Order` of spring-data,
     *
     * For example: cast to list whose element type is `Sort.Order` of spring data
     *
     * <pre><code>
     * List&lt;Sort.Order&gt; orders =
     *     Order.makeCustomOrders(
     *         (path, orderMode, nullOrderMode) -> {
     *             NullHandling nullHandling;
     *             switch (nullOrderMode) {
     *                 case NullOrderMode.NULLS_FIRST:
     *                     nullHandling = NullHandling.NULLS_FIRST;
     *                     break;
     *                 case NullOrderMode.NULLS_LAST:
     *                     nullHandling = NullHandling.NULLS_LAST;
     *                     break;
     *                 default:
     *                     nullHandling = NullHandling.NATIVE;
     *                     break;
     *             }
     *             return new Sort.Order(
     *                 path,
     *                 orderMode == OrderMode.DESC ? Direction.DESC : Direction.ASC,
     *                 nullHandling
     *             );
     *         },
     *         "name asc, parent.name desc nulls last"
     *         "edition desc",
     *         "region.segment.size; region.segment.cost desc"
     *     )
     * );
     * </code></pre>
     */
    public static <O> List<O> makeCustomOrders(
            CustomOrderCreator<O> creator,
            String... codes
    ) {
        List<O> orders = new ArrayList<>();
        for (String code : codes) {
            for (String part : SEPARATOR_PATTERN.split(code)) {
                part = part.trim();
                if (!part.isEmpty()) {
                    orders.add(makeCustomOrder(part, creator));
                }
            }
        }
        return orders;
    }

    private static <O> O makeCustomOrder(String code, CustomOrderCreator<O> creator) {

        String[] parts = WHITESPACE_PATTERN.split(code);
        OrderMode orderMode = OrderMode.ASC;
        NullOrderMode nullOrderMode = NullOrderMode.UNSPECIFIED;

        // 0: asc | desc, 1: nulls, 2: first | last, 3: end
        int channel = 0;

        for (int i = 1; i < parts.length; i++) {
            String rest = parts[i].toLowerCase();
            switch (channel) {
                case 0:
                    switch (rest) {
                        case "asc":
                            channel = 1;
                            break;
                        case "desc":
                            channel = 1;
                            orderMode = OrderMode.DESC;
                            break;
                        case "nulls":
                            channel = 2;
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Illegal order code \"" + code + "\"," +
                                            "\"asc\", \"desc\" or \"nulls\" is expected after property path"
                            );
                    }
                    break;
                case 1:
                    if ("nulls".equals(rest)) {
                        channel = 2;
                    } else {
                        throw new IllegalArgumentException(
                                "Illegal order code \"" + code + "\"" +
                                        "\"nulls\" is expected but \"" + rest + "\" is found"
                        );
                    }
                    break;
                case 2:
                    switch (rest) {
                        case "first":
                            channel = 3;
                            nullOrderMode = NullOrderMode.NULLS_FIRST;
                            break;
                        case "last":
                            channel = 3;
                            nullOrderMode = NullOrderMode.NULLS_LAST;
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Illegal order code \"" + code + "\"," +
                                            "\"asc\", \"desc\" or \"nulls\" is expected after property path"
                            );
                    }
                    break;
                case 3:
                    throw new IllegalArgumentException(
                            "Illegal order code \"" + code + "\"," +
                                    "unexpected token \"" + rest + "\""
                    );
            }
        }
        if (channel == 2) {
            throw new IllegalArgumentException(
                    "Illegal order code \"" + code + "\", " +
                            "\"first\" or \"last\" is expected after \"nulls\""
            );
        }
        return creator.create(parts[0], orderMode, nullOrderMode);
    }

    public static Expression<?> orderedExpression(Props table, String path) {
        List<ImmutableProp> props = orderedPropChain(table.getImmutableType(), path);
        boolean allNullable = props.stream().filter(it -> it.isReference(TargetLevel.PERSISTENT)).allMatch(ImmutableProp::isNullable);
        Props source = table;
        Expression<?> expr = null;
        for (ImmutableProp prop : props) {
            if (prop.isReference(TargetLevel.PERSISTENT)) {
                source = source.join(prop.getName(), allNullable ? JoinType.LEFT : JoinType.INNER);
            } else if (expr != null) {
                expr = ((PropExpression.Embedded<?>)expr).get(prop.getName());
            } else {
                expr = source.get(prop.getName());
            }
        }
        if (expr == null) {
            throw new AssertionError("Internal bug, illegal path \"" + path + "\"");
        }
        return expr;
    }

    public static List<ImmutableProp> orderedPropChain(ImmutableType type, String path) {
        List<ImmutableProp> props = new ArrayList<>();
        while (!path.isEmpty()) {
            int dotIndex = path.indexOf('.');
            String propName = dotIndex == -1 ? path : path.substring(0, dotIndex);
            String restPath = dotIndex == -1 ? "" : path.substring(dotIndex + 1);
            ImmutableProp prop = type.getProps().get(propName);
            if (prop == null) {
                throw new IllegalArgumentException(
                        "Cannot resolve ordered property path \"" +
                                path +
                                "\" from \"" +
                                type +
                                "\", there is no property \"" +
                                propName +
                                "\" in \"" +
                                type +
                                "\""
                );
            }
            if (prop.isReferenceList(TargetLevel.OBJECT) || prop.isScalarList()) {
                throw new IllegalArgumentException(
                        "Cannot resolve ordered property path \"" +
                                path +
                                "\" from \"" +
                                type +
                                "\", the property \"" +
                                prop +
                                "\" cannot be list"
                );
            }
            if (restPath.isEmpty() && !prop.isScalar(TargetLevel.OBJECT)) {
                throw new IllegalArgumentException(
                        "Cannot resolve ordered property path \"" +
                                path +
                                "\" from \"" +
                                type +
                                "\", \"" +
                                prop +
                                "\" is the last property of the path but it is not scalar"
                );
            }
            if (!restPath.isEmpty() && !prop.isReference(TargetLevel.PERSISTENT) && !prop.isEmbedded(EmbeddedLevel.BOTH)) {
                throw new IllegalArgumentException(
                        "Cannot resolve ordered property path \"" +
                                path +
                                "\" from \"" +
                                type +
                                "\", \"" +
                                prop +
                                "\" is not the last property of path but it is neither reference nor embedded"
                );
            }
            props.add(prop);
            path = restPath;
            if (prop.getTargetType() != null) {
                type = prop.getTargetType();
            }
        }
        return props;
    }

    public interface CustomOrderCreator<O> {
        O create(String path, OrderMode orderMode, NullOrderMode nullOrderMode);
    }
}
