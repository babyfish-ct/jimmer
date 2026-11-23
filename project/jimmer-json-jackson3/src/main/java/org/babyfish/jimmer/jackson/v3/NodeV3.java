package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.json.Node;
import tools.jackson.databind.JsonNode;

import java.util.*;

public class NodeV3 implements Node {

    private final JsonNode node;

    public NodeV3(JsonNode node) {
        this.node = node;
    }

    @Override
    public Node get(final int index) {
        JsonNode jsonNode = node.get(index);
        return jsonNode != null ? new NodeV3(jsonNode) : null;
    }

    @Override
    public Node get(final String fieldName) {
        JsonNode jsonNode = node.get(fieldName);
        return jsonNode != null ? new NodeV3(jsonNode) : null;
    }

    @Override
    public Iterator<Map.Entry<String, Node>> fieldsIterator() {
        return new NodePropertiesIteratorV3(node.properties().iterator());
    }

    @Override
    public Kind kind() {
        return Kind.valueOf(node.getNodeType().name());
    }

    @Override
    public int size() {
        return node.size();
    }

    @Override
    public Object toJavaValue() {
        switch (kind()) {
            case OBJECT:
                Map<String, Object> map = new LinkedHashMap<>();
                fieldsIterator().forEachRemaining(e -> map.put(e.getKey(), e.getValue().toJavaValue()));
                return map;
            case ARRAY:
                List<Object> list = new ArrayList<>(size());
                for (int i = 0; i < size(); i++) {
                    list.add(get(i).toJavaValue());
                }
                return list;
            case NUMBER:
                return node.numberValue();
            case BOOLEAN:
                return node.booleanValue();
            case STRING:
                return node.stringValue();
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Node && contentEquals((Node) o);
    }

    @Override
    public int hashCode() {
        return contentHashCode();
    }

    @Override
    public String toString() {
        return node.toString();
    }
}
