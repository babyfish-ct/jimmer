package org.babyfish.jimmer.json;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.babyfish.jimmer.json.JsonTestProviders.CODECS;
import static org.junit.jupiter.api.Assertions.*;

public class CodecContractTest {

    @Test
    public void testCallerOwnsStreamsAndNumericTreeIsExact() throws Exception {
        String input = "{\"字\":[9007199254740993,1.2345678901234567890123456789,null]}";
        for (JsonCodec codec : CODECS) {
            String name = codec.getClass().getSimpleName();
            ByteArrayInputStream source = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public void close() {
                    fail(name + " closed caller's input");
                }
            };
            Node object = codec.treeReader().read(source);
            assertEquals(Node.Kind.OBJECT, object.kind());
            assertNull(object.get("missing"));
            Node array = object.get("字");
            assertEquals(3, array.size());
            assertEquals(Node.Kind.NULL, array.get(2).kind());
            assertEquals(Long.valueOf(9007199254740993L), array.get(0).castTo(Long.class), name);
            assertThrows(ArithmeticException.class, () -> array.get(0).castTo(Integer.class));
            assertEquals(new BigDecimal("1.2345678901234567890123456789"), array.get(1).castTo(BigDecimal.class), name);
            ByteArrayOutputStream target = new ByteArrayOutputStream() {
                @Override
                public void close() {
                    fail(name + " closed caller's output");
                }
            };
            JsonType<List<Long>> longs = JsonType.listOf(JsonType.of(long.class));
            codec.writer(longs).write(target, List.of(9007199254740993L));
            assertEquals("[9007199254740993]", target.toString("UTF-8"), name);
            assertEquals(List.of(9007199254740993L), codec.reader(longs).read(target.toByteArray()), name);
        }
    }
}
