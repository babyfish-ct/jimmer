package org.babyfish.jimmer.sql.microservice;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.model.JimmerModule;
import org.babyfish.jimmer.sql.model.microservice.*;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.MicroServiceExchange;
import org.babyfish.jimmer.sql.runtime.MicroServiceExporter;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class MicroServiceQueryTest extends AbstractQueryTest {

    @Test
    public void testFetchManyToOne() {

        OrderItemTable table = OrderItemTable.$;
        JSqlClient sqlClient = getSqlClient(builder ->
                builder
                        .setMicroServiceName("order-item-service")
                        .setMicroServiceExchange(new MicroServiceExchangeImpl())
        );
        executeAndExpect(
                sqlClient
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        OrderItemFetcher.$
                                                .allScalarFields()
                                                .order(
                                                        OrderFetcher.$
                                                                .allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.ORDER_ID " +
                                    "from MS_ORDER_ITEM as tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"ms-order-1.item-1\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":1," +
                                    "--->--->--->\"name\":\"ms-order-1\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"ms-order-1.item-2\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":1," +
                                    "--->--->--->\"name\":\"ms-order-1\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":3," +
                                    "--->--->\"name\":\"ms-order-2.item-1\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":2," +
                                    "--->--->--->\"name\":\"ms-order-2\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":4," +
                                    "--->--->\"name\":\"ms-order-2.item-2\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":2," +
                                    "--->--->--->\"name\":\"ms-order-2\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFetchOneToMany() {
        OrderTable table = OrderTable.$;
        JSqlClient sqlClient = getSqlClient(builder ->
                builder
                        .setMicroServiceName("order-service")
                        .setMicroServiceExchange(new MicroServiceExchangeImpl())
        );
        executeAndExpect(
                sqlClient
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        OrderFetcher.$
                                                .allScalarFields()
                                                .orderItems(
                                                        OrderItemFetcher.$
                                                                .allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from MS_ORDER as tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"ms-order-1\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"ms-order-1.item-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"ms-order-1.item-2\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"ms-order-2\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"ms-order-2.item-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":4," +
                                    "--->--->--->--->\"name\":\"ms-order-2.item-2\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFetchManyToMany() {
        OrderItemTable table = OrderItemTable.$;
        JSqlClient sqlClient = getSqlClient(builder ->
                builder
                        .setMicroServiceName("order-item-service")
                        .setMicroServiceExchange(new MicroServiceExchangeImpl())
        );
        executeAndExpect(
                sqlClient
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        OrderItemFetcher.$
                                                .allScalarFields()
                                                .products(
                                                        ProductFetcher.$
                                                                .allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from MS_ORDER_ITEM as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ORDER_ITEM_ID, tb_1_.PRODUCT_ID " +
                                    "from MS_ORDER_ITEM_PRODUCT_MAPPING as tb_1_ " +
                                    "where tb_1_.ORDER_ITEM_ID in (?, ?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"ms-order-1.item-1\"," +
                                    "--->--->\"products\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"ms-product-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"ms-product-2\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"ms-order-1.item-2\"," +
                                    "--->--->\"products\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"ms-product-2\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"ms-product-3\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":3," +
                                    "--->--->\"name\":\"ms-order-2.item-1\"," +
                                    "--->--->\"products\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"ms-product-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"ms-product-3\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":4," +
                                    "--->--->\"name\":\"ms-order-2.item-2\"," +
                                    "--->--->\"products\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"ms-product-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"ms-product-2\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"ms-product-3\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFetchReversedManyToOne() {
        ProductTable table = ProductTable.$;
        JSqlClient sqlClient = getSqlClient(builder ->
                builder
                        .setMicroServiceName("product-service")
                        .setMicroServiceExchange(new MicroServiceExchangeImpl())
        );
        executeAndExpect(
                sqlClient
                        .createQuery(table)
                        .select(
                                table.fetch(
                                        ProductFetcher.$
                                                .allScalarFields()
                                                .orderItems(
                                                        OrderItemFetcher.$
                                                                .allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from MS_PRODUCT as tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"ms-product-1\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"ms-order-1.item-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"ms-order-2.item-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":4," +
                                    "--->--->--->--->\"name\":\"ms-order-2.item-2\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"ms-product-2\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":1," +
                                    "--->--->--->--->\"name\":\"ms-order-1.item-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"ms-order-1.item-2\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":4," +
                                    "--->--->--->--->\"name\":\"ms-order-2.item-2\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":3," +
                                    "--->--->\"name\":\"ms-product-3\"," +
                                    "--->--->\"orderItems\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":2," +
                                    "--->--->--->--->\"name\":\"ms-order-1.item-2\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":3," +
                                    "--->--->--->--->\"name\":\"ms-order-2.item-1\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":4," +
                                    "--->--->--->--->\"name\":\"ms-order-2.item-2\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    private static class MicroServiceExchangeImpl implements MicroServiceExchange {

        private static final ConnectionManager CONNECTION_MANAGER =
                new ConnectionManager() {
                    @Override
                    public <R> R execute(Function<Connection, R> block) {
                        R[] ref = (R[])new Object[1];
                        jdbc(con -> {
                            ref[0] = block.apply(con);
                        });
                        return ref[0];
                    }
                };

        private final JSqlClient orderClient =
                JSqlClient
                        .newBuilder()
                        .setEntityManager(JimmerModule.ENTITY_MANAGER)
                        .setConnectionManager(CONNECTION_MANAGER)
                        .setMicroServiceName("order-service")
                        .setMicroServiceExchange(this)
                        .build();

        private final JSqlClient orderItemClient =
                JSqlClient
                        .newBuilder()
                        .setEntityManager(JimmerModule.ENTITY_MANAGER)
                        .setConnectionManager(CONNECTION_MANAGER)
                        .setMicroServiceName("order-item-service")
                        .setMicroServiceExchange(this)
                        .build();

        private final JSqlClient productClient =
                JSqlClient
                        .newBuilder()
                        .setEntityManager(JimmerModule.ENTITY_MANAGER)
                        .setConnectionManager(CONNECTION_MANAGER)
                        .setMicroServiceName("product-service")
                        .setMicroServiceExchange(this)
                        .build();

        @SuppressWarnings("unchecked")
        @Override
        public List<ImmutableSpi> findByIds(
                String microServiceName,
                Collection<?> ids,
                Fetcher<?> fetcher
        ) {
            return new MicroServiceExporter(sqlClient(microServiceName))
                    .findByIds(ids, fetcher);
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
                String microServiceName,
                ImmutableProp prop,
                Collection<?> targetIds,
                Fetcher<?> fetcher
        ) {
            return new MicroServiceExporter(sqlClient(microServiceName))
                    .findByAssociatedIds(prop, targetIds, fetcher);
        }

        private JSqlClient sqlClient(String microServiceName) {
            switch (microServiceName) {
                case "order-service":
                    return orderClient;
                case "order-item-service":
                    return orderItemClient;
                case "product-service":
                    return productClient;
                default:
                    throw new IllegalArgumentException(
                            "Illegal microservice name \"" +
                                    microServiceName +
                                    "\""
                    );
            }
        }
    }
}
