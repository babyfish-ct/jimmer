package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;
import static org.babyfish.jimmer.sql.common.Constants.*;

import java.math.BigDecimal;

public class DMLWithTriggerTest extends AbstractTriggerTest {

    @Test
    public void testUpdate() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createUpdate(book)
                        .set(book.price(), book.price().plus(BigDecimal.ONE))
                        .where(book.name().eq("GraphQL in Action")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                        it.variables("GraphQL in Action");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK tb_1_ " +
                                        "set PRICE = tb_1_.PRICE + ? " +
                                        "where tb_1_.ID in(?, ?, ?) " +
                                        "and tb_1_.NAME = ?"
                        );
                        it.unorderedVariables(
                                BigDecimal.ONE,
                                graphQLInActionId1, graphQLInActionId2, graphQLInActionId3,
                                "GraphQL in Action"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.id in(?, ?, ?)"
                        );
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.rowCount(3);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId3 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId3 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":81.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId1 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId1 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":81.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId2 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":81.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId2 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":82.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }
}
