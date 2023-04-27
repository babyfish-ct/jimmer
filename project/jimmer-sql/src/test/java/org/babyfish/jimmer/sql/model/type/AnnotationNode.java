package org.babyfish.jimmer.sql.model.type;

import java.util.Map;

public class AnnotationNode {

    private final String typeName;

    private final Map<String, Object> arguments;

    public AnnotationNode(String typeName, Map<String, Object> arguments) {
        this.typeName = typeName;
        this.arguments = arguments;
    }

    public String getTypeName() {
        return typeName;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return "AnnotationTag{" +
                "typeName='" + typeName + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}
