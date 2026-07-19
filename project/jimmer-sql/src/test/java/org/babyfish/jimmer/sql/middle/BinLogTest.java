package org.babyfish.jimmer.sql.middle;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.sql.JSqlClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;

public class BinLogTest {

    private static final JsonCodec<?> JSON_CODEC = jsonCodec();

    private JSqlClient sqlClient;

    private List<String> events;

    @BeforeEach
    public void initialize() {
        sqlClient = JSqlClient
                .newBuilder()
                .build();
        events = new ArrayList<>();
        sqlClient.getTriggers().addAssociationListener(e -> {
            StringBuilder builder = new StringBuilder();
            builder
                    .append(e.getImmutableProp().getDeclaringType().getJavaClass().getSimpleName())
                    .append('.')
                    .append(e.getImmutableProp().getName())
                    .append(": ");
            builder.append(e.getSourceId());
            if (e.getDetachedTargetId() != null) {
                builder.append(" - ").append(e.getDetachedTargetId());
            }
            if (e.getAttachedTargetId() != null) {
                builder.append(" + ").append(e.getAttachedTargetId());
            }
            events.add(builder.toString());
        });
    }

    @Test
    public void testLogicalDeletion() throws Exception {
        sqlClient.getBinLog().accept(
                "[SHOP_customer_mappING]",
                JSON_CODEC.treeReader().read("{" +
                        "\"[shop_ID]\": 1," +
                        "\"`CUSTOMER_ID`\": 1," +
                        "\"DELETED_millis\": 0," +
                        "\"[tyPE]\": \"VIP\"" +
                        "}"
                ),
                JSON_CODEC.treeReader().read("{" +
                        "\"[deleted_Millis]\": 1" +
                        "}"
                )
        );
        Assertions.assertEquals(
                "[Customer.vipShops: 1 - 1, " +
                        "Shop.vipCustomers: 1 - 1, " +
                        "Shop.customers: 1 - 1, " +
                        "Customer.shops: 1 - 1]",
                events.toString()
        );
    }

    @Test
    public void tetLogicalInsertion() throws Exception {
        sqlClient.getBinLog().accept(
                "[SHOP_customer_mappING]",
                JSON_CODEC.treeReader().read("{" +
                        "\"[shop_ID]\": 1," +
                        "\"`CUSTOMER_ID`\": 4," +
                        "\"DELETED_millis\": -1," +
                        "\"[tyPE]\": \"ORDINARY\"" +
                        "}"
                ),
                JSON_CODEC.treeReader().read("{" +
                        "\"[deleted_Millis]\": 0" +
                        "}"
                )
        );
        Assertions.assertEquals(
                "[Shop.customers: 1 + 4, " +
                        "Customer.shops: 4 + 1, " +
                        "Shop.ordinaryCustomers: 1 + 4, " +
                        "Customer.ordinaryShops: 4 + 1]",
                events.toString()
        );
    }

    @Test
    public void testChangeType() throws Exception {
        sqlClient.getBinLog().accept(
                "[SHOP_customer_mappING]",
                JSON_CODEC.treeReader().read("{" +
                        "\"[shop_ID]\": 1," +
                        "\"`CUSTOMER_ID`\": 1," +
                        "\"DELETED_millis\": 0," +
                        "\"[tyPE]\": \"VIP\"" +
                        "}"
                ),
                JSON_CODEC.treeReader().read("{" +
                        "\"`TypE`\": \"ORDINARY\"" +
                        "}"
                )
        );
        Assertions.assertEquals(
                "[Customer.vipShops: 1 - 1, " +
                        "Shop.vipCustomers: 1 - 1, " +
                        "Shop.customers: 1 - 1, " +
                        "Customer.shops: 1 - 1, " +
                        "Shop.customers: 1 + 1, " +
                        "Customer.shops: 1 + 1, " +
                        "Shop.ordinaryCustomers: 1 + 1, " +
                        "Customer.ordinaryShops: 1 + 1]",
                events.toString()
        );
    }

    @Test
    public void testChangeEverything() throws Exception {
        sqlClient.getBinLog().accept(
                "[SHOP_customer_mappING]",
                JSON_CODEC.treeReader().read("{" +
                        "\"[shop_ID]\": 1," +
                        "\"`CUSTOMER_ID`\": 1," +
                        "\"DELETED_millis\": 0," +
                        "\"[tyPE]\": \"VIP\"" +
                        "}"
                ),
                JSON_CODEC.treeReader().read("{" +
                        "\"[shop_ID]\": 1," +
                        "\"`CUSTOMER_ID`\": 9," +
                        "\"[tyPE]\": \"ORDINARY\"" +
                        "}"
                )
        );
        Assertions.assertEquals(
                "[Customer.vipShops: 1 - 1, " +
                        "Shop.vipCustomers: 1 - 1, " +
                        "Shop.customers: 1 - 1, " +
                        "Customer.shops: 1 - 1, " +
                        "Shop.customers: 1 + 9, " +
                        "Customer.shops: 9 + 1, " +
                        "Shop.ordinaryCustomers: 1 + 9, " +
                        "Customer.ordinaryShops: 9 + 1]",
                events.toString()
        );
    }
}
