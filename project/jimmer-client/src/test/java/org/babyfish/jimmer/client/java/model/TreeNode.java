package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongToStringConverter;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.OneToMany;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The tree node entity
 */
@Entity
public interface TreeNode {

    /**
     * The id of tree node.
     *
     * <p>It doesn't make business sense, it's just auto-numbering.</p>
     */
    @JsonConverter(LongToStringConverter.class)
    @Id
    long id();

    /**
     * The name of current tree node
     *
     * <p>Together with `parent`, this property forms the key of the book</p>
     */
    String name();

    /**
     * The many-to-one association from `TreeNode` to `TreeNode`
     *
     * <p>Together with `name`, this property forms the key of the book</p>
     */
    @ManyToOne
    @Nullable
    TreeNode parent();

    /**
     * The one-to-many association from `TreeNode` to `TreeNode`,
     * it is opposite mirror of `TreeNode.parent`
     */
    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();
}
