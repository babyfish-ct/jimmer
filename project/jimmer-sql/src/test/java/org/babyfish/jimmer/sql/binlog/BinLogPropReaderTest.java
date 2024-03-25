package org.babyfish.jimmer.sql.binlog;

import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogImpl;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogParser;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.inheritance.Administrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

public class BinLogPropReaderTest {

    private static final Schema BOOK_PRICE_SCHEMA = Decimal.schema(2);

    private final BinLogParser parser =
            ((BinLogImpl) JSqlClient
                    .newBuilder()
                    .setBinLogPropReader(LocalDateTime.class, (prop, jsonNode) ->
                            Instant.ofEpochMilli(Long.parseLong(jsonNode.asText()) / 1000)
                                    .atOffset(ZoneOffset.ofHours(8)).toLocalDateTime())
                    .setBinLogPropReader(BookProps.PRICE, (prop, jsonNode) ->
                            Decimal.toLogical(
                                    BOOK_PRICE_SCHEMA,
                                    Base64.getDecoder().decode(jsonNode.asText())
                            ))
                    .build()
                    .getBinLog()
            ).parser();

    @Test
    public void testByProp() {
        Book book = parser.parseEntity(
                Book.class,
                "{\n" +
                "      \"id\": \"b649b11b-1161-4ad2-b261-af0112fdd7c8\",\n" +
                "      \"name\": \"Learning GraphQL\",\n" +
                "      \"edition\": 2,\n" +
                "      \"price\": \"FXw=\",\n" +
                "      \"store_id\": \"2fa3955e-3e83-49b9-902e-0465c109c779\"\n" +
                "   }"
        );
        Assertions.assertEquals(
                "{" +
                "\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                "\"name\":\"Learning GraphQL\"," +
                "\"edition\":2," +
                "\"price\":55.00," +
                "\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                "}",
                book.toString()
        );
    }

    @Test
    public void testByType() {
        Administrator administrator = parser.parseEntity(
                Administrator.class,
                "{" +
                "\"created_time\": 1688419256525125,\n" +
                "\"modified_time\": 1688419256525125" +
                "}"
        );
        Assertions.assertEquals(
                "{" +
                "\"createdTime\":\"2023-07-04 05:20:56\"," +
                "\"modifiedTime\":\"2023-07-04 05:20:56\"" +
                "}",
                administrator.toString()
        );
    }
}
