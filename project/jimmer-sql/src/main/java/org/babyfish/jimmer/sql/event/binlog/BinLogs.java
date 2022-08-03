package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BinLogs {

    private BinLogs() {}

    private static final ObjectMapper binlogMapper =
            new ObjectMapper().registerModule(new BinLogModule());

    public static <T> T parse(Class<T> type, String json) {
        try {
            return binlogMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Illegal json: " + json, ex);
        }
    }
}
