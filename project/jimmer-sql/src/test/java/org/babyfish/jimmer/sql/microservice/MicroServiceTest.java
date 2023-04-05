package org.babyfish.jimmer.sql.microservice;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.microservice.OrderFetcher;
import org.babyfish.jimmer.sql.model.microservice.OrderItemFetcher;
import org.babyfish.jimmer.sql.model.microservice.OrderItemTable;
import org.junit.jupiter.api.Test;

public class MicroServiceTest extends AbstractQueryTest {

    @Test
    public void testFetchManyToOne() {

        OrderItemTable table = OrderItemTable.$;
        JSqlClient sqlClient = getSqlClient(builder ->
                builder.setMicroServiceName("item-service")
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
                    ctx.sql("");
                }
        );
    }
}
