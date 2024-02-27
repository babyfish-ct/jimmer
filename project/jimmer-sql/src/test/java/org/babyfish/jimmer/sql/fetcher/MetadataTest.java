package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetadataTest {

    @Test
    public void testBookFetcher() {
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.Book { id, name, edition, price }",
                BookFetcher.$.allScalarFields().toString()
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.Book { id, name, edition, price, storeId }",
                BookFetcher.$.allTableFields().toString()
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.Book { id, name, edition, store { id } }",
                BookFetcher.$
                        .allScalarFields()
                        .store().store(false).store()
                        .price(false).price().price(false)
                        .toString()
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.Book { " +
                        "id, " +
                        "store(batchSize: 128) { id, name }, " +
                        "authors(batchSize: 1, limit: 100) { id, firstName, lastName } " +
                        "}",
                BookFetcher.$
                        .store(BookStoreFetcher.$.name(), it -> it.batch(128))
                        .authors(AuthorFetcher.$.firstName().lastName(), it -> it.batch(1).limit(100))
                        .toString()
        );
    }

    @Test
    public void testTreeNodeFetcher() {
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.TreeNode { " +
                        "id, " +
                        "name, " +
                        "parent(depth: 5), " +
                        "childNodes(depth: 10) " +
                        "}",
                TreeNodeFetcher.$
                        .name()
                        .recursiveParent(it -> it.depth(5))
                        .recursiveChildNodes(it -> it.depth(10))
                        .toString()
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.TreeNode { " +
                        "id, " +
                        "name, " +
                        "parent(recursive: true), " +
                        "childNodes(recursive: true) " +
                        "}",
                TreeNodeFetcher.$
                        .name()
                        .recursiveParent()
                        .recursiveChildNodes()
                        .toString()
        );
    }

    @Test
    public void testMultiPropFetcherFields() {
        Fetcher<TreeNode> fetcher =
                TreeNodeFetcher.$
                        .name()
                        .recursiveParent()
                        .recursiveChildNodes();
        Assertions.assertEquals(
                "{id=id, name=name, parent=parent(recursive: true), childNodes=childNodes(recursive: true)}",
                fetcher.getFieldMap().toString()
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.TreeNode { id, name, parent }",
                        fetcher.getFieldMap().get("parent").getChildFetcher().toString()
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.TreeNode { id, name }",
                fetcher.getFieldMap().get("childNodes").getChildFetcher().toString()
        );
    }
}
