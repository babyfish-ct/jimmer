package org.babyfish.jimmer.sql.binlog;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.binlog.impl.BinLogParser;
import org.babyfish.jimmer.sql.event.binlog.impl.MiddleRow;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.inheritance.Role;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class BinLogTest {

    private final BinLogParser parser =
            new BinLogParser().initialize(
                    (JSqlClientImplementor) JSqlClient.newBuilder()
                            .build(),
                    null,
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );

    @Test
    public void testTreeNode() {
        String json = "{\"Node_Id\": 2, \"[Name]\": 3, \"`Parent_Id`\": 1}";
        TreeNode treeNode = parser.parseEntity(TreeNode.class, json);
        Assertions.assertEquals(
                "{\"id\":2,\"name\":\"3\",\"parent\":{\"id\":1}}",
                treeNode.toString()
        );
    }

    @Test
    public void testRootTreeNode() {
        String json = "{\"Node_Id\": 1, \"[Name]\": \"Root\", \"`Parent_Id`\": null}";
        TreeNode treeNode = parser.parseEntity(TreeNode.class, json);
        Assertions.assertEquals(
                "{\"id\":1,\"name\":\"Root\",\"parent\":null}",
                treeNode.toString()
        );
    }

    @Test
    public void testAuthor() {
        String json = "{\"id\": \"" +
                alexId +
                "\", \"[First_name]\": \"Alex\", \"[last_Name]\": \"Banks\", \"`GenDER`\": \"M\"}";
        Author author = parser.parseEntity(Author.class, json);
        Assertions.assertEquals(
                "{\"id\":\"" +
                        alexId +
                        "\",\"firstName\":\"Alex\",\"lastName\":\"Banks\",\"gender\":\"MALE\"}",
                author.toString()
        );
    }

    @Test
    public void testRole() {
        String json = "{" +
                "\"id\": 1, " +
                "\"name\": \"Role\", " +
                "\"created_time\": \"2022-10-03 00:00:00\"," +
                "\"modified_time\": \"2022-10-03 00:10:00\"" +
                "}";
        Role role = parser.parseEntity(Role.class, json);
        Assertions.assertEquals(
                "{\"name\":\"Role\",\"id\":1}",
                role.toString()
        );
    }

    @Test
    public void testAssociation() {
        String json = "{\"[Book_Id]\": \"" +
                learningGraphQLId1 +
                "\", \"`Author_Id`\": \"" +
                danId +
                "\"}";
        MiddleRow<UUID, UUID> middleRow = parser.parseMiddleRow(BookProps.AUTHORS, json);
        Assertions.assertEquals(learningGraphQLId1, middleRow.sourceId);
        Assertions.assertEquals(danId, middleRow.targetId);
    }

    @Test
    public void testInverseAssociation() {
        String json = "{\"[Book_Id]\": \"" +
                learningGraphQLId1 +
                "\", \"`Author_Id`\": \"" +
                danId +
                "\"}";
        MiddleRow<UUID, UUID> middleRow = parser.parseMiddleRow(AuthorProps.BOOKS, json);
        Assertions.assertEquals(danId, middleRow.sourceId);
        Assertions.assertEquals(learningGraphQLId1, middleRow.targetId);
    }
}
