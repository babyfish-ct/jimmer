package org.babyfish.jimmer.jackson.v3;

import tools.jackson.databind.JsonNode;

/**
 * @deprecated 请使用 {@link org.babyfish.jimmer.json.jackson.v3.NodeV3}。
 */
@Deprecated
public class NodeV3 extends org.babyfish.jimmer.json.jackson.v3.NodeV3 {

    public NodeV3(JsonNode node) {
        super(node);
    }
}
