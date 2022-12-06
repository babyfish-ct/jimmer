package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.binlog.BinLogParser;
import org.babyfish.jimmer.sql.model.embedded.Transform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BinLogTest {

    private final JSqlClient sqlClient = JSqlClient.newBuilder().build();

    @Test
    public void testTransform() {
        String json = "{" +
                "\"`Id`\": 1, " +
                "\"Left\": 100, \"[Top]\": 120, \"`RIGHT`\": 400, \"bottom\": 320, " +
                "\"target_Left\": 600, \"[TARGET_Top]\": 800, \"`target_RIGHT`\": 900, \"Target_bottom\": 100" +
                "}";
        Transform transform = new BinLogParser().initialize(sqlClient).parseEntity(Transform.class, json);
        assertJson(
                "{" +
                        "--->\"id\":1," +
                        "--->\"source\":{" +
                        "--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                        "--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                        "--->}," +
                        "--->\"target\":{" +
                        "--->--->\"leftTop\":{\"x\":600,\"y\":800}," +
                        "--->--->\"rightBottom\":{\"x\":900,\"y\":100}" +
                        "--->}" +
                        "}",
                transform.toString()
        );
    }

    @Test
    public void testTransformWithNull() {
        String json = "{" +
                "\"`Id`\": 1, " +
                "\"Left\": 100, \"[Top]\": 120, \"`RIGHT`\": 400, \"bottom\": 320, " +
                "\"target_Left\": 600, \"[TARGET_Top]\": 800, \"`target_RIGHT`\": null, \"Target_bottom\": 100" +
                "}";
        Transform transform = new BinLogParser().initialize(sqlClient).parseEntity(Transform.class, json);
        assertJson(
                "{" +
                        "--->\"id\":1," +
                        "--->\"source\":{" +
                        "--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                        "--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                        "--->}," +
                        "--->\"target\":null" +
                        "}",
                transform.toString()
        );
    }

    private static void assertJson(String expected, String actual) {
        Assertions.assertEquals(
                expected
                        .replace("--->", "")
                        .replace("\r", "")
                        .replace("\n", ""),
                actual
        );
    }
}
