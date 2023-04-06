package org.babyfish.jimmer.sql.microservice;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.model.JimmerModule;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.MicroServiceExchange;
import org.babyfish.jimmer.sql.runtime.MicroServiceExporter;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class MicroServiceExchangeImpl implements MicroServiceExchange {

    private static final ConnectionManager CONNECTION_MANAGER =
            new ConnectionManager() {
                @SuppressWarnings("unchecked")
                @Override
                public <R> R execute(Function<Connection, R> block) {
                    R[] ref = (R[])new Object[1];
                    AbstractTest.jdbc(con -> {
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
