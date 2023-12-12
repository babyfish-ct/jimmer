package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.client.java.model.Fetchers;
import org.babyfish.jimmer.client.java.model.Tree;
import org.babyfish.jimmer.client.java.model.TreeNode;
import org.babyfish.jimmer.client.java.model.TreeNodeFetcher;
import org.babyfish.jimmer.client.meta.Api;
import org.babyfish.jimmer.sql.fetcher.RecursiveListFieldConfig;

@Api("treeService")
public interface TreeService {

    @Api
    Tree<Integer> getNumberTree();

    @Api
    Tree<String> getStringTree();

    @Api
    @FetchBy("RECURSIVE_FETCHER") TreeNode getRootNode();

    TreeNodeFetcher RECURSIVE_FETCHER =
            Fetchers.TREE_NODE_FETCHER
                    .allScalarFields()
                    .childNodes(
                            Fetchers.TREE_NODE_FETCHER
                                    .allScalarFields(),
                            RecursiveListFieldConfig::recursive
                    );
}
