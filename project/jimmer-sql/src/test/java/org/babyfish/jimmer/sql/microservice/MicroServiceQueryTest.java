package org.babyfish.jimmer.sql.microservice;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.model.JimmerModule;
import org.babyfish.jimmer.sql.model.microservice.*;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.MicroServiceExchange;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

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

        private final JSqlClient orderClient =
                JSqlClient
                        .newBuilder()
                        .setEntityManager(JimmerModule.ENTITY_MANAGER)
                        .setMicroServiceName("order-service")
                        .setMicroServiceExchange(this)
                        .build();

        private final JSqlClient orderItemClient =
                JSqlClient
                        .newBuilder()
                        .setEntityManager(JimmerModule.ENTITY_MANAGER)
                        .setMicroServiceName("order-item-service")
                        .setMicroServiceExchange(this)
                        .build();

        private final JSqlClient productClient =
                JSqlClient
                        .newBuilder()
                        .setEntityManager(JimmerModule.ENTITY_MANAGER)
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
            List<ImmutableSpi>[] ref = new List[1];
            jdbc(con ->
                    ref[0] = sqlClient(microServiceName)
                            .getEntities()
                            .forConnection(con)
                            .findByIds((Fetcher<ImmutableSpi>) fetcher, ids)
            );
            return ref[0];
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
                String microServiceName,
                ImmutableProp prop,
                Collection<?> targetIds,
                Fetcher<?> fetcher
        ) {
            List<Tuple2<Object, ImmutableSpi>>[] ref = new List[1];
            jdbc(con -> {
                MutableRootQueryImpl<Table<ImmutableSpi>> query =
                        new MutableRootQueryImpl<>(
                                sqlClient(microServiceName),
                                prop.getDeclaringType(),
                                ExecutionPurpose.LOADER,
                                false
                        );
                PropExpression<Object> targetIdExpr = query
                        .getTable()
                        .join(prop.getName())
                        .get(prop.getTargetType().getIdProp().getName());
                query.where(targetIdExpr.in((Collection<Object>) targetIds));
                query.freeze();
                ref[0] = query.select(
                        targetIdExpr,
                        ((Table<ImmutableSpi>)query.getTable()).fetch(
                                (Fetcher<ImmutableSpi>) fetcher
                        )
                ).execute(con);
            });
            return ref[0];
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
