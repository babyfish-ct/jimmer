package org.babyfish.jimmer.jackson.v3;

import org.babyfish.jimmer.jackson.codec.JsonConverter;
import org.babyfish.jimmer.jackson.codec.Node;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class NodeV3 implements Node {
    private static final Map<Class<?>, Function<JsonNode, ?>> CASTER_MAP = new HashMap<>();

    static {
        CASTER_MAP.put(boolean.class, JsonNode::asBoolean);
        CASTER_MAP.put(Boolean.class, JsonNode::asBoolean);
        CASTER_MAP.put(char.class, JsonNode::asString);
        CASTER_MAP.put(Character.class, JsonNode::asString);
        CASTER_MAP.put(byte.class, JsonNode::asInt);
        CASTER_MAP.put(Byte.class, JsonNode::asInt);
        CASTER_MAP.put(short.class, JsonNode::asInt);
        CASTER_MAP.put(Short.class, JsonNode::asInt);
        CASTER_MAP.put(int.class, JsonNode::asInt);
        CASTER_MAP.put(Integer.class, JsonNode::asInt);
        CASTER_MAP.put(long.class, JsonNode::asLong);
        CASTER_MAP.put(Long.class, JsonNode::asLong);
        CASTER_MAP.put(float.class, JsonNode::asDouble);
        CASTER_MAP.put(Float.class, JsonNode::asDouble);
        CASTER_MAP.put(double.class, JsonNode::asDouble);
        CASTER_MAP.put(Double.class, JsonNode::asDouble);
        CASTER_MAP.put(BigInteger.class, JsonNode::asInt);
        CASTER_MAP.put(BigDecimal.class, JsonNode::asInt);
        CASTER_MAP.put(String.class, JsonNode::asString);
        CASTER_MAP.put(UUID.class, valueNode -> UUID.fromString(valueNode.asString()));
    }

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
    public boolean isNull() {
        return node.isNull();
    }

    @Override
    public boolean canCastTo(Class<?> type) {
        return CASTER_MAP.containsKey(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T castTo(Class<T> type) {
        Function<JsonNode, ?> caster = CASTER_MAP.get(type);
        if (caster == null) {
            throw new IllegalArgumentException("Cannot cast node to type " + type.getName());
        }
        return (T) caster.apply(node);
    }

    @Override
    public <T> T convertTo(Class<T> targetType, JsonConverter converter) throws Exception {
        return converter.convert(node, targetType);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass() && node.equals(((NodeV3) o).node);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public String toString() {
        return node.toString();
    }
}
