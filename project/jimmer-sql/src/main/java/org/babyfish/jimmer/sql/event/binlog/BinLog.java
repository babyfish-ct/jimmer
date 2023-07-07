package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.databind.JsonNode;

public interface BinLog {

    void accept(String tableName, JsonNode oldData, JsonNode newData);

    void accept(String tableName, JsonNode oldData, JsonNode newData, String reason);
}
