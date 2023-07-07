package org.babyfish.jimmer.sql.event.binlog;

import com.fasterxml.jackson.databind.JsonNode;
import org.babyfish.jimmer.meta.ImmutableProp;

@FunctionalInterface
public interface BinLogPropReader {

    Object read(ImmutableProp prop, JsonNode jsonNode);
}
