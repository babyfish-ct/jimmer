package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.NotNull;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Objects;

public class ExportedSavePath {

    private final String rootTypeName;

    private final List<Node> nodes;

    @ConstructorProperties({"rootTypeName", "nodes"})
    public ExportedSavePath(String rootTypeName, List<Node> nodes) {
        this.rootTypeName = rootTypeName;
        this.nodes = nodes;
    }

    public String getRootTypeName() {
        return rootTypeName;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("<root>");
        for (Node node : nodes) {
            builder.append('.').append(node.prop);
        }
        return builder.toString();
    }

    public static class Node {

        private final String prop;

        private final String targetTypeName;

        @ConstructorProperties({"prop", "targetTypeName"})
        public Node(
                @NotNull String prop,
                @NotNull String targetTypeName) {
            this.prop = Objects.requireNonNull(prop, "prop");
            this.targetTypeName = Objects.requireNonNull(targetTypeName, "targetTypeName");
        }

        @NotNull
        public String getProp() {
            return prop;
        }

        @NotNull
        public String getTargetTypeName() {
            return targetTypeName;
        }

        @Override
        public String toString() {
            return "JsonPathNode{" +
                    "prop='" + prop + '\'' +
                    ", targetTypeName='" + targetTypeName + '\'' +
                    '}';
        }
    }
}
