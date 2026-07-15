package org.babyfish.jimmer.jackson.v2;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v2.NodeV2}。
 */
@Deprecated
public class NodeV2 extends org.babyfish.jimmer.json.jackson.v2.NodeV2 {

    public NodeV2(JsonNode node) {
        super(node);
    }
}
