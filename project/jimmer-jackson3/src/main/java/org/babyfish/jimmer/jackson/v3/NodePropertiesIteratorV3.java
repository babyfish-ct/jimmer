package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Map;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v3.NodePropertiesIteratorV3}。
 */
@Deprecated
public class NodePropertiesIteratorV3 extends org.babyfish.jimmer.json.jackson.v3.NodePropertiesIteratorV3 {

    public NodePropertiesIteratorV3(Iterator<Map.Entry<String, JsonNode>> nodeIterator) {
        super(nodeIterator);
    }
}
