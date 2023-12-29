package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.common.GetMapping;
import org.babyfish.jimmer.client.java.model.Fetchers;
import org.babyfish.jimmer.client.java.model.Tree;
import org.babyfish.jimmer.client.java.model.TreeNode;
import org.babyfish.jimmer.client.java.model.TreeNodeFetcher;
import org.babyfish.jimmer.client.java.model.dto.SimpleTreeNodeView;
import org.babyfish.jimmer.client.meta.Api;
import org.babyfish.jimmer.client.common.RequestParam;
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig;

import java.util.List;

/**
 * This is the service to test,
 * it can return two kinds of trees:
 *
 * <ul>
 *     <li>Recursive static object: Tree</li>
 *     <li>Recursive fetched object: TreeNode</li>
 * </ul>
 */
@Api("treeService")
public interface TreeService {

    /**
     * Create a static object tree, the value of each node must be integer.
     * @param depth The depth of the tree
     * @param breadth The child count of each tree node
     * @return The static object tree with integer values.
     */
    @Api
    @GetMapping("/numberTree")
    Tree<Integer> getNumberTree(
            @RequestParam Integer depth,
            @RequestParam Integer breadth
    );

    /**
     * Create a static object tree, the value of each node must be integer.
     * @param depth The depth of the tree
     * @param breadth The child count of each tree node
     * @param maxBound The max bound for the random integer value which is data of each node
     * @return The static object tree with integer values.
     */
    @Api
    @GetMapping("/numberTree2")
    Tree<Integer> getNumberTree(
            @RequestParam Integer depth,
            @RequestParam Integer breadth,
            @RequestParam(defaultVale = "10") int maxBound
    );

    /**
     * Create a static object tree, the value of each node must be string.
     * @param depth The depth of the tree
     * @param breadth The child count of each tree node
     * @return The static object tree with string values.
     */
    @Api
    @GetMapping("/stringTree")
    Tree<String> getStringTree(
            @RequestParam Integer depth,
            @RequestParam Integer breadth
    );

    /**
     * Create query recursive tree roots by optional node name.
     * @param name The optional string value to filter root nodes.
     * @return The fetched object tree
     */
    @Api
    @GetMapping("/rootNode")
    @FetchBy("RECURSIVE_FETCHER") TreeNode getRootNode(
            @RequestParam(defaultVale = "X") String name
    );

    @Api
    @GetMapping("/rootNode/simple")
    List<SimpleTreeNodeView> getSimpleRootNodes();

    /**
     * Recursive tree node, for business scenarios: A, B and C
     */
    TreeNodeFetcher RECURSIVE_FETCHER =
            Fetchers.TREE_NODE_FETCHER
                    .allScalarFields()
                    .childNodes(
                            Fetchers.TREE_NODE_FETCHER
                                    .allScalarFields(),
                            RecursiveListFieldConfig::recursive
                    );
}
