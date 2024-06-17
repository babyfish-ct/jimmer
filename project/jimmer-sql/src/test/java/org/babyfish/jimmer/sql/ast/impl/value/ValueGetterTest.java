package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.OrderItemProps;
import org.babyfish.jimmer.sql.model.embedded.OrderItemTable;
import org.babyfish.jimmer.sql.model.embedded.TransformTable;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class ValueGetterTest extends AbstractQueryTest {

    @Test
    public void testExpression() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        BookTable table = BookTable.$;
        List<ValueGetter> getters = ValueGetter.valueGetters(sqlClient, table.name(), null);
        Assertions.assertEquals(
                "[NAME]",
                getters.stream().map(ValueGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList("GraphQL in Action"),
                getters.stream().map(it -> it.get("GraphQL in Action")).collect(Collectors.toList())
        );
    }

    @Test
    public void testReferenceExpression() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        BookTable table = BookTable.$;
        List<ValueGetter> getters = ValueGetter.valueGetters(sqlClient, table.storeId(), null);
        Assertions.assertEquals(
                "[STORE_ID]",
                getters.stream().map(ValueGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(manningId),
                getters.stream().map(it -> it.get(manningId)).collect(Collectors.toList())
        );
    }

    @Test
    public void testEmbedded() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        TransformTable table = TransformTable.$;
        List<ValueGetter> getters = ValueGetter.valueGetters(sqlClient, table.source(), null);
        Assertions.assertEquals(
                "[`LEFT`, TOP, `RIGHT`, BOTTOM]",
                getters.stream().map(ValueGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(1L, 4L, 9L, 16L),
                getters.stream().map(it -> it.get(Objects.createRect(source -> {
                    source.applyLeftTop(lt -> lt.setX(1).setY(4));
                    source.applyRightBottom(rb -> rb.setX(9).setY(16));
                }))).collect(Collectors.toList())
        );
    }

    @Test
    public void testPartialEmbedded() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        TransformTable table = TransformTable.$;
        List<ValueGetter> getters = ValueGetter.valueGetters(sqlClient, table.source().rightBottom(), null);
        Assertions.assertEquals(
                "[`RIGHT`, BOTTOM]",
                getters.stream().map(ValueGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(9L, 16L),
                getters.stream().map(it -> it.get(Objects.createPoint(rb -> {
                    rb.setX(9).setY(16);
                }))).collect(Collectors.toList())
        );
    }

    @Test
    public void testDeepPartialEmbedded() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        TransformTable table = TransformTable.$;
        List<ValueGetter> getters = ValueGetter.valueGetters(sqlClient, table.source().rightBottom().y(), null);
        Assertions.assertEquals(
                "[BOTTOM]",
                getters.stream().map(ValueGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(16L),
                getters.stream().map(it -> it.get(16L)).collect(Collectors.toList())
        );
    }

    @Test
    public void testEmbeddedReference() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        OrderItemTable table = OrderItemTable.$;
        List<ValueGetter> getters = ValueGetter.valueGetters(sqlClient, table.orderId(), null);
        Assertions.assertEquals(
                "[FK_ORDER_X, FK_ORDER_Y]",
                getters.stream().map(ValueGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList("X_", "Y_"),
                getters.stream().map(
                        it -> it.get(Objects.createOrderId(id -> id.setX("X_").setY("Y_")))
                ).collect(Collectors.toList())
        );
    }

    @Test
    public void testPartialEmbeddedReference() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        OrderItemTable table = OrderItemTable.$;
        List<ValueGetter> getters = ValueGetter.valueGetters(sqlClient, table.order().id().y(), null);
        Assertions.assertEquals(
                "[FK_ORDER_Y]",
                getters.stream().map(ValueGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList("Y_"),
                getters.stream().map(it -> it.get("Y_")).collect(Collectors.toList())
        );
    }

    @Test
    public void testMiddleTable() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        AssociationType type = AssociationType.of(BookProps.AUTHORS.unwrap());
        List<ValueGetter> getters = ValueGetter.tupleGetters(
                AbstractValueGetter.createValueGetters(
                        sqlClient,
                        type.getSourceProp(),
                        null
                ),
                AbstractValueGetter.createValueGetters(
                        sqlClient,
                        type.getTargetProp(),
                        null
                )
        );
        Assertions.assertEquals(
                "[BOOK_ID, AUTHOR_ID]",
                getters.stream().map(ValueGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(manningId, graphQLInActionId3),
                getters.stream().map(it -> {
                    return it.get(new Tuple2<>(manningId, graphQLInActionId3));
                }).collect(Collectors.toList())
        );
    }

    @Test
    public void testEmbeddedMiddleTable() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        AssociationType type = AssociationType.of(OrderItemProps.PRODUCTS.unwrap());
        List<ValueGetter> getters = ValueGetter.tupleGetters(
                AbstractValueGetter.createValueGetters(
                        sqlClient,
                        type.getSourceProp(),
                        null
                ),
                AbstractValueGetter.createValueGetters(
                        sqlClient,
                        type.getTargetProp(),
                        null
                )
        );
        Assertions.assertEquals(
                "[FK_ORDER_ITEM_A, FK_ORDER_ITEM_B, FK_ORDER_ITEM_C, FK_PRODUCT_ALPHA, FK_PRODUCT_BETA]",
                getters.stream().map(ValueGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(1, 4, 9, "Alpha_", "Beta_"),
                getters.stream().map(it -> {
                    return it.get(
                            new Tuple2<>(
                                    Objects.createOrderItemId(id -> id.setA(1).setB(4).setC(9)),
                                    Objects.createProductId(id -> id.setAlpha("Alpha_").setBeta("Beta_"))
                            )
                    );
                }).collect(Collectors.toList())
        );
    }
}
