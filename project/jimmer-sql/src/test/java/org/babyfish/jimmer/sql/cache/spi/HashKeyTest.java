package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.jackson.v2.JsonCodecV2;
import org.babyfish.jimmer.json.JsonWriter;
import org.babyfish.jimmer.json.gson.GsonCodec;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HashKeyTest {

    @Test
    public void gsonReadsHistoricalHashFieldsAndPreservesLiteralEscapes() throws Exception {
        JsonWriter<Object> historical = new JsonCodecV2().writer();
        JsonWriter<Object> gson = new GsonCodec().writer();
        String line = Character.toString((char) 0x2028);
        String paragraph = Character.toString((char) 0x2029);
        for (String value : Arrays.asList(
                "<tag>&=\"'", line, paragraph, "\\u2028", "\\u2029",
                "\\" + line, "\\" + paragraph, "\\\\u2028", "\\\\u2029",
                "\\\\" + line, "\\\\" + paragraph, line + paragraph + "\\u2028" + "\\" + paragraph
        )) {
            SortedMap<String, Object> parameters = new TreeMap<>();
            parameters.put("region", value);
            assertEquals("{\"region\":" + historical.writeAsString(value) + "}",
                    AbstractRemoteHashBinder.hashKey(parameters, gson));
        }
    }
}
