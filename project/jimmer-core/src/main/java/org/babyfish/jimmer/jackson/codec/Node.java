package org.babyfish.jimmer.jackson.codec;

import java.util.Iterator;
import java.util.Map;

public interface Node {

    Node get(int index);

    Node get(String fieldName);

    Iterator<Map.Entry<String, Node>> fieldsIterator();

    boolean isNull();

    boolean canCastTo(Class<?> type);

    <T> T castTo(Class<T> type);

    <T> T convertTo(Class<T> targetType, JsonConverter converter) throws Exception;
}
