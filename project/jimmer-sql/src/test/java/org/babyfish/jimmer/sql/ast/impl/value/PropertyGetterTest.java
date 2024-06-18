package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.embedded.OrderItem;
import org.babyfish.jimmer.sql.model.embedded.OrderItemDraft;
import org.babyfish.jimmer.sql.model.embedded.Transform;
import org.babyfish.jimmer.sql.model.embedded.TransformDraft;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class PropertyGetterTest extends AbstractQueryTest {

    @Test
    public void testAllFields() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setName("GraphQL in Action");
            draft.setEdition(2);
            draft.setPrice(new BigDecimal("49.99"));
            draft.setStoreId(manningId);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(learningGraphQLId3);
            draft.setName("Learning GraphQL");
            draft.setEdition(3);
            draft.setPrice(new BigDecimal("39.99"));
            draft.setStoreId(oreillyId);
        });
        List<PropertyGetter> getters = PropertyGetter.entityGetters(
                (JSqlClientImplementor) getSqlClient(),
                ImmutableType.get(Book.class),
                null,
                false
        );
        Assertions.assertEquals(
                "[id, name, edition, price, store.id]",
                getters.toString()
        );
        Assertions.assertEquals(
                Arrays.asList(graphQLInActionId2, "GraphQL in Action", 2, new BigDecimal("49.99"), manningId),
                getters.stream().map(it -> it.get(book1)).collect(Collectors.toList())
        );
        Assertions.assertEquals(
                "[ID, NAME, EDITION, PRICE, STORE_ID]",
                getters.stream().map(PropertyGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(learningGraphQLId3, "Learning GraphQL", 3, new BigDecimal("39.99"), oreillyId),
                getters.stream().map(it -> it.get(book2)).collect(Collectors.toList())
        );
    }

    @Test
    public void testPartialFields() {
        Book book1 = BookDraft.$.produce(draft -> {
            draft.setId(graphQLInActionId2);
            draft.setStoreId(manningId);
        });
        Book book2 = BookDraft.$.produce(draft -> {
            draft.setId(learningGraphQLId3);
            draft.setStoreId(oreillyId);
        });
        List<PropertyGetter> getters = PropertyGetter.entityGetters(
                (JSqlClientImplementor) getSqlClient(),
                ImmutableType.get(Book.class),
                (ImmutableSpi) book1,
                false
        );
        Assertions.assertEquals(
                "[id, store.id]",
                getters.toString()
        );
        Assertions.assertEquals(
                Arrays.asList(graphQLInActionId2, manningId),
                getters.stream().map(it -> it.get(book1)).collect(Collectors.toList())
        );
        Assertions.assertEquals(
                "[ID, STORE_ID]",
                getters.stream().map(PropertyGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(learningGraphQLId3, oreillyId),
                getters.stream().map(it -> it.get(book2)).collect(Collectors.toList())
        );
    }

    @Test
    public void testEmbeddedScalar() {
        Transform transform = TransformDraft.$.produce(draft -> {
            draft.setId(13L);
            draft.applySource(source -> {
                source.applyLeftTop(lt -> lt.setX(1).setY(4));
            });
            draft.applyTarget(target -> {
                target.applyRightBottom(rb -> rb.setX(9).setY(16));
            });
        });
        List<PropertyGetter> getters = PropertyGetter.entityGetters(
                (JSqlClientImplementor) getSqlClient(),
                ImmutableType.get(Transform.class),
                (ImmutableSpi) transform,
                false
        );
        Assertions.assertEquals(
                "[id, source.leftTop.x, source.leftTop.y, target.rightBottom.x, target.rightBottom.y]",
                getters.toString()
        );
        Assertions.assertEquals(
                "[ID, `LEFT`, TOP, TARGET_RIGHT, TARGET_BOTTOM]",
                getters.stream().map(PropertyGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(13L, 1L, 4L, 9L, 16L),
                getters.stream().map(it -> it.get(transform)).collect(Collectors.toList())
        );
    }

    @Test
    public void testEmbeddedReference() {
        OrderItem orderItem = OrderItemDraft.$.produce(draft -> {
            draft.setId(
                   Objects.createOrderItemId(id -> id.setA(1).setB(4).setC(9))
            );
            draft.setOrderId(
                    Objects.createOrderId(id -> id.setX("Bob").setY("Dylan"))
            );
        });
        List<PropertyGetter> getters = PropertyGetter.entityGetters(
                (JSqlClientImplementor) getSqlClient(),
                ImmutableType.get(OrderItem.class),
                (ImmutableSpi) orderItem,
                false
        );
        Assertions.assertEquals(
                "[id.a, id.b, id.c, order.id.x, order.id.y]",
                getters.toString()
        );
        Assertions.assertEquals(
                "[ORDER_ITEM_A, ORDER_ITEM_B, ORDER_ITEM_C, FK_ORDER_X, FK_ORDER_Y]",
                getters.stream().map(PropertyGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(1, 4, 9, "Bob", "Dylan"),
                getters.stream().map(it -> it.get(orderItem)).collect(Collectors.toList())
        );
    }

    @Test
    public void testPartialEmbeddedReference() {
        OrderItem orderItem = OrderItemDraft.$.produce(draft -> {
            draft.setId(
                    Objects.createOrderItemId(id -> id.setA(1).setB(4))
            );
            draft.setOrderId(
                    Objects.createOrderId(id -> id.setX("Bob"))
            );
        });
        List<PropertyGetter> getters = PropertyGetter.entityGetters(
                (JSqlClientImplementor) getSqlClient(),
                ImmutableType.get(OrderItem.class),
                (ImmutableSpi) orderItem,
                false
        );
        Assertions.assertEquals(
                "[id.a, id.b, order.id.x]",
                getters.toString()
        );
        Assertions.assertEquals(
                "[ORDER_ITEM_A, ORDER_ITEM_B, FK_ORDER_X]",
                getters.stream().map(PropertyGetter::columnName).collect(Collectors.toList()).toString()
        );
        Assertions.assertEquals(
                Arrays.asList(1, 4, "Bob"),
                getters.stream().map(it -> it.get(orderItem)).collect(Collectors.toList())
        );
    }
}
