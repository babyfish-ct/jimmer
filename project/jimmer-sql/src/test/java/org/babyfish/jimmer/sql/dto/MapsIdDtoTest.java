package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.mapsid.MapsIdMessageDeliveryTable;
import org.babyfish.jimmer.sql.model.mapsid.dto.MapsIdMessageDeliveryView;
import org.junit.jupiter.api.Test;

public class MapsIdDtoTest extends AbstractQueryTest {

    @Test
    public void testFetchFullScalarMappedIdWithAssociationIdName() {
        MapsIdMessageDeliveryTable table = MapsIdMessageDeliveryTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.messageId().eq(100L))
                        .select(table.fetch(MapsIdMessageDeliveryView.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.MESSAGE_ID, tb_1_.STATUS, tb_1_.MESSAGE_ID " +
                                    "from MAPS_ID_MESSAGE_DELIVERY tb_1_ " +
                                    "where tb_1_.MESSAGE_ID = ?"
                    );
                    ctx.variables(100L);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.TEXT " +
                                    "from MAPS_ID_MESSAGE tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.statement(1).variables(100L);
                    ctx.rows(rows -> {
                        assertContentEquals(
                                "[" +
                                        "--->MapsIdMessageDeliveryView(" +
                                        "--->--->messageId=100, " +
                                        "--->--->status=SENT, " +
                                        "--->--->message=MapsIdMessageDeliveryView.TargetOf_message(" +
                                        "--->--->--->id=100, " +
                                        "--->--->--->text=Hello" +
                                        "--->--->)" +
                                        "--->)" +
                                        "]",
                                rows
                        );
                    });
                }
        );
    }
}
