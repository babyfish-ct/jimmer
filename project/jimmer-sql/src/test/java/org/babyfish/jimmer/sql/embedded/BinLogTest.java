package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.binlog.BinLogParser;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.embedded.OrderItem;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.embedded.ProductProps;
import org.babyfish.jimmer.sql.model.embedded.Transform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.danId;
import static org.babyfish.jimmer.sql.common.Constants.learningGraphQLId1;

public class BinLogTest {

    private final JSqlClient sqlClient = JSqlClient.newBuilder().build();

    @Test
    public void testTransform() {
        String json = "{" +
                "\"`Id`\": 1, " +
                "\"Left\": 100, \"[Top]\": 120, \"`RIGHT`\": 400, \"bottom\": 320, " +
                "\"target_Left\": 600, \"[TARGET_Top]\": 800, \"`target_RIGHT`\": 900, \"Target_bottom\": 100" +
                "}";
        Transform transform = new BinLogParser().initialize(sqlClient).parseEntity(Transform.class, json);
        assertJson(
                "{" +
                        "--->\"id\":1," +
                        "--->\"source\":{" +
                        "--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                        "--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                        "--->}," +
                        "--->\"target\":{" +
                        "--->--->\"leftTop\":{\"x\":600,\"y\":800}," +
                        "--->--->\"rightBottom\":{\"x\":900,\"y\":100}" +
                        "--->}" +
                        "}",
                transform.toString()
        );
    }

    @Test
    public void testTransformWithNull() {
        String json = "{" +
                "\"`Id`\": 1, " +
                "\"Left\": 100, \"[Top]\": 120, \"`RIGHT`\": 400, \"bottom\": 320, " +
                "\"target_Left\": 600, \"[TARGET_Top]\": 800, \"`target_RIGHT`\": null, \"Target_bottom\": 100" +
                "}";
        Transform transform = new BinLogParser().initialize(sqlClient).parseEntity(Transform.class, json);
        assertJson(
                "{" +
                        "--->\"id\":1," +
                        "--->\"source\":{" +
                        "--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                        "--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                        "--->}," +
                        "--->\"target\":null" +
                        "}",
                transform.toString()
        );
    }

    @Test
    public void testAssociation() {
        String json = "{" +
                "\"[fk_ORDER_item_A]\": 3, \"`fk_order_item_b`\": 2, \"FK_ORDER_ITEM_c\": 1, " +
                "\"FK_product_ALPHA\": \"00X\", \"FK_product_BETA\": \"00Y\"" +
                "}";
        Tuple2<Long, Long> idPair = new BinLogParser().initialize(sqlClient)
                .parseIdPair(
                        OrderItemProps.PRODUCTS,
                        json
                );
        Assertions.assertEquals(
                "Tuple2(_1={\"a\":3,\"b\":2,\"c\":1}, _2={\"alpha\":\"00X\",\"beta\":\"00Y\"})",
                idPair.toString()
        );
    }

    @Test
    public void testInverseAssociation() {
        String json = "{" +
                "\"[fk_ORDER_item_A]\": 3, \"`fk_order_item_b`\": 2, \"FK_ORDER_ITEM_c\": 1, " +
                "\"FK_product_ALPHA\": \"00X\", \"FK_product_BETA\": \"00Y\"" +
                "}";
        Tuple2<Long, Long> idPair = new BinLogParser().initialize(sqlClient)
                .parseIdPair(
                        ProductProps.ORDER_ITEMS,
                        json
                );
        Assertions.assertEquals(
                "Tuple2(_1={\"alpha\":\"00X\",\"beta\":\"00Y\"}, _2={\"a\":3,\"b\":2,\"c\":1})",
                idPair.toString()
        );
    }

    @Test
    public void testNonNullForeignKey() {
        String json = "{" +
                "\"[order_item_a]\": 10, \"[order_item_b]\": 11, \"[order_item_c]\": 12, " +
                "\"[name]\": \"X-order\", \"`fk_order_X`\": \"010\", \"`FK_Order_y`\": \"020\"" +
                "}";
        OrderItem orderItem = new BinLogParser().initialize(sqlClient).parseEntity(OrderItem.class, json);
        assertJson(
                "{" +
                        "--->\"id\":{\"a\":10,\"b\":11,\"c\":12}," +
                        "--->\"name\":\"X-order\"," +
                        "--->\"order\":{" +
                        "--->--->\"id\":{\"x\":\"010\",\"y\":\"020\"}" +
                        "--->}" +
                        "}",
                orderItem.toString()
        );
    }

    @Test
    public void testNullForeignKey() {
        String json = "{" +
                "\"[order_item_a]\": 10, \"[order_item_b]\": 11, \"[order_item_c]\": 12, " +
                "\"[name]\": \"X-order\", \"`fk_order_X`\": null" +
                "}";
        OrderItem orderItem = new BinLogParser().initialize(sqlClient).parseEntity(OrderItem.class, json);
        assertJson(
                "{" +
                        "--->\"id\":{\"a\":10,\"b\":11,\"c\":12}," +
                        "--->\"name\":\"X-order\"," +
                        "--->\"order\":null" +
                        "}",
                orderItem.toString()
        );
    }

    private static void assertJson(String expected, String actual) {
        Assertions.assertEquals(
                expected
                        .replace("--->", "")
                        .replace("\r", "")
                        .replace("\n", ""),
                actual
        );
    }
}
