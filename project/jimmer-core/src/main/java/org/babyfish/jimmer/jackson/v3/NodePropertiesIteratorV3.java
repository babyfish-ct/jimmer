package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.jackson.codec.Node;
import tools.jackson.databind.JsonNode;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

public class NodePropertiesIteratorV3 implements Iterator<Map.Entry<String, Node>> {
    private final Iterator<Map.Entry<String, JsonNode>> nodeIterator;

    public NodePropertiesIteratorV3(Iterator<Map.Entry<String, JsonNode>> nodeIterator) {
        this.nodeIterator = nodeIterator;
    }

    @Override
    public boolean hasNext() {
        return nodeIterator.hasNext();
    }

    @Override
    public void remove() {
        nodeIterator.remove();
    }

    @Override
    public Map.Entry<String, Node> next() {
        Map.Entry<String, JsonNode> entry = nodeIterator.next();
        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), new NodeV3(entry.getValue()));
    }
}
