package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.model.TreeNodeFetcher;
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
                "org.babyfish.jimmer.sql.model.Book { id, name, edition, price, store }",
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
                        "parent(depth: 5) { id, name, parent { id } }, " +
                        "childNodes(depth: 10) { id, name } " +
                        "}",
                TreeNodeFetcher.$
                        .parent(TreeNodeFetcher.$.name(), it -> it.depth(5))
                        .childNodes(TreeNodeFetcher.$.name(), it -> it.depth(10))
                        .toString()
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.TreeNode { " +
                        "id, " +
                        "parent(recursive: true) { id, name, parent { id } }, " +
                        "childNodes(recursive: true) { id, name } " +
                        "}",
                TreeNodeFetcher.$
                        .parent(TreeNodeFetcher.$.name(), RecursiveFieldConfig::recursive)
                        .childNodes(TreeNodeFetcher.$.name(), RecursiveFieldConfig::recursive)
                        .toString()
        );
    }
}
