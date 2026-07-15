package org.babyfish.jimmer.sql.event.binlog;

import org.babyfish.jimmer.json.codec.Node;
import org.babyfish.jimmer.meta.ImmutableProp;

@FunctionalInterface
public interface BinLogPropReader {

    Object read(ImmutableProp prop, Node node);
}
