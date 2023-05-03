package org.babyfish.jimmer.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.sql.Serialized;

import java.util.List;
import java.util.Map;

@Immutable
public interface TreeNode {

    String name();

    @JsonProperty("TheParentNode")
    TreeNode parent();

    @JsonAlias({"children", "all-child-nodes"})
    List<TreeNode> childNodes();
}
