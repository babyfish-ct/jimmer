package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Map;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v2.NodePropertiesIteratorV2}。
 */
@Deprecated
public class NodePropertiesIteratorV2 extends org.babyfish.jimmer.json.jackson.v2.NodePropertiesIteratorV2 {

    public NodePropertiesIteratorV2(Iterator<Map.Entry<String, JsonNode>> nodeIterator) {
        super(nodeIterator);
    }
}
