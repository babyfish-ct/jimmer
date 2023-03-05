package org.babyfish.jimmer.sql.runtime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ExportedSavePath {

    private final String rootTypeName;

    private final List<Node> nodes;

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

        @JsonCreator
        public Node(
                @JsonProperty(value = "prop", required = true) @NotNull String prop,
                @JsonProperty(value = "targetTypeName", required = true) @NotNull String targetTypeName) {
            this.prop = prop;
            this.targetTypeName = targetTypeName;
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
