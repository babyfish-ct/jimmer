package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.babyfish.jimmer.sql.model.exclude.User;
import org.babyfish.jimmer.sql.model.exclude.UserFetcher;
import org.babyfish.jimmer.sql.model.exclude.UserProps;
import org.babyfish.jimmer.sql.model.exclude.dto.ExcludedUserView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

public class MetadataTest {

    @Test
    public void testBookFetcher() {
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.Book { id, name, edition, price }",
                BookFetcher.$.allScalarFields().toString()
        );
        Assertions.assertEquals(
                "org.babyfish.jimmer.sql.model.Book { id, name, edition, price, storeId, @implicit store }",
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

    @Test
    public void testFormulaBaseOnEmbeddable() {
        Fetcher<Machine> fetcher =
                MachineFetcher.$
                        .factoryCount()
                        .factoryNames()
                        .detail(
                                MachineDetailFetcher.$
                                        .patents()
                        );
        Tests.assertContentEquals(
                "org.babyfish.jimmer.sql.model.embedded.Machine { " +
                        "--->id, " +
                        "--->factoryCount, " +
                        "--->factoryNames, " +
                        "--->detail { " +
                        "--->--->patents, " +
                        "--->--->@implicit factories " +
                        "--->} " +
                        "}",
                fetcher.toString()
        );
    }

    @Test
    public void testFormulaBaseOnEmbeddableAndDuplicatedFetching() {
        Fetcher<Machine> fetcher =
                MachineFetcher.$
                        .factoryCount()
                        .detail(
                                MachineDetailFetcher.$
                                        .patents()
                                        .factories()
                        );
        Tests.assertContentEquals(
                "org.babyfish.jimmer.sql.model.embedded.Machine { " +
                        "--->id, " +
                        "--->factoryCount, " +
                        "--->detail { " +
                        "--->--->patents, " +
                        "--->--->factories " +
                        "--->} " +
                        "}",
                fetcher.toString()
        );
    }

    @Test
    public void testFormulaInEmbeddable() {
        Fetcher<Transform> fetcher =
                TransformFetcher.$
                        .source(
                                RectFetcher.$
                                        .area()
                        )
                        .target(
                                RectFetcher.$
                                        .area()
                        );
        Tests.assertContentEquals(
                "org.babyfish.jimmer.sql.model.embedded.Transform { " +
                        "--->id, " +
                        "--->source { " +
                        "--->--->area, " +
                        "--->--->@implicit leftTop { " +
                        "--->--->--->@implicit x, " +
                        "--->--->--->@implicit y " +
                        "--->--->}, " +
                        "--->--->@implicit rightBottom { " +
                        "--->--->--->@implicit x, " +
                        "--->--->--->@implicit y " +
                        "--->--->} " +
                        "--->}, " +
                        "--->target { " +
                        "--->--->area, " +
                        "--->--->@implicit leftTop { " +
                        "--->--->--->@implicit x, " +
                        "--->--->--->@implicit y " +
                        "--->--->}, " +
                        "--->--->@implicit rightBottom { " +
                        "--->--->--->@implicit x, " +
                        "--->--->--->@implicit y " +
                        "--->--->} " +
                        "--->} " +
                        "}",
                fetcher.toString()
        );
    }

    @Test
    public void testFormulaInEmbeddableAndDuplicatedFetching() {
        Fetcher<Transform> fetcher =
                TransformFetcher.$
                        .source(
                                RectFetcher.$
                                        .area()
                                        .leftTop(PointFetcher.$.x())
                        )
                        .target(
                                RectFetcher.$
                                        .area()
                                        .rightBottom(PointFetcher.$.y())
                        );
        Tests.assertContentEquals(
                "org.babyfish.jimmer.sql.model.embedded.Transform { " +
                        "--->id, " +
                        "--->source { " +
                        "--->--->area, " +
                        "--->--->leftTop { " +
                        "--->--->--->x, " +
                        "--->--->--->@implicit y " +
                        "--->--->}, " +
                        "--->--->@implicit rightBottom { " +
                        "--->--->--->@implicit x, " +
                        "--->--->--->@implicit y " +
                        "--->--->} " +
                        "--->}, " +
                        "--->target { " +
                        "--->--->area, " +
                        "--->--->rightBottom { " +
                        "--->--->--->@implicit x, " +
                        "--->--->--->y " +
                        "--->--->}, " +
                        "--->--->@implicit leftTop { " +
                        "--->--->--->@implicit x, " +
                        "--->--->--->@implicit y " +
                        "--->--->} " +
                        "--->} " +
                        "}",
                fetcher.toString()
        );
    }

    @Test
    public void testTestFormulaBaseOnAssociation() {
        Fetcher<Book> fetcher = BookFetcher.$
                .name()
                .authorFullNames();
        Tests.assertContentEquals(
                "org.babyfish.jimmer.sql.model.Book { " +
                        "--->id, " +
                        "--->name, " +
                        "--->authorFullNames, " +
                        "--->@implicit authors { " +
                        "--->--->id, " +
                        "--->--->@implicit firstName, " +
                        "--->--->@implicit lastName " +
                        "--->} " +
                        "}",
                fetcher.toString()
        );
    }

    @Test
    public void testTestFormulaBaseOnAssociationAndDuplicatedFetcher() {
        Fetcher<Book> fetcher = BookFetcher.$
                .name()
                .authorFullNames()
                .authors(
                        AuthorFetcher.$.lastName().gender()
                );
        Tests.assertContentEquals(
                "org.babyfish.jimmer.sql.model.Book { " +
                        "--->id, " +
                        "--->name, " +
                        "--->authorFullNames, " +
                        "--->authors { " +
                        "--->--->id, " +
                        "--->--->gender, " +
                        "--->--->@implicit firstName, " +
                        "--->--->lastName " +
                        "--->} " +
                        "}",
                fetcher.toString()
        );
    }

    @Test
    public void testUser() {

        Fetcher<User> userFetcher = UserFetcher.$.allScalarFields();
        Tests.assertContentEquals(
                "org.babyfish.jimmer.sql.model.exclude.User { " +
                        "--->id, " +
                        "--->name, " +
                        "--->nickName " +
                        "}",
                userFetcher
        );

        Tests.assertContentEquals(
                "org.babyfish.jimmer.sql.model.exclude.User { " +
                        "--->id, " +
                        "--->name, " +
                        "--->nickName " +
                        "}",
                ExcludedUserView.METADATA.getFetcher()
        );

        java.lang.reflect.Field name;
        try {
            name = ExcludedUserView.class.getDeclaredField(UserProps.NAME.unwrap().getName());
        } catch (NoSuchFieldException e) {
            name = null;
        }
        Assertions.assertNotNull(name);

        java.lang.reflect.Field password;
        try {
            password = ExcludedUserView.class.getDeclaredField(UserProps.PASSWORD.unwrap().getName());
        } catch (NoSuchFieldException e) {
            password = null;
        }
        Assertions.assertNull(password);
    }

    @Test
    public void testVersion() {
        Assertions.assertTrue(BookStoreProps.VERSION.unwrap().getDefaultValueRef() != null);
    }
}
