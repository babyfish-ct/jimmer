package org.babyfish.jimmer.sql.binlog;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.binlog.BinLogParser;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.inheritance.Role;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class BinLogTest {

    private JSqlClient sqlClient = JSqlClient.newBuilder().build();

    @Test
    public void testTreeNode() {
        String json = "{\"Node_Id\": 2, \"[Name]\": 3, \"`Parent_Id`\": 1}";
        TreeNode treeNode = new BinLogParser().initialize(sqlClient).parseEntity(TreeNode.class, json);
        Assertions.assertEquals(
                "{\"id\":2,\"name\":\"3\",\"parent\":{\"id\":1}}",
                treeNode.toString()
        );
    }

    @Test
    public void testAuthor() {
        String json = "{\"id\": \"" +
                alexId +
                "\", \"[First_name]\": \"Alex\", \"[last_Name]\": \"Banks\", \"`GenDER`\": \"M\"}";
        Author author = new BinLogParser().initialize(sqlClient).parseEntity(Author.class, json);
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
        Role role = new BinLogParser().initialize(sqlClient).parseEntity(Role.class, json);
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
        Tuple2<Long, Long> idPair = new BinLogParser().initialize(sqlClient)
                .parseIdPair(
                        BookProps.AUTHORS,
                        json
                );
        Assertions.assertEquals(
                "Tuple2(_1=" +
                        learningGraphQLId1 +
                        ", _2=" +
                        danId +
                        ")",
                idPair.toString()
        );
    }

    @Test
    public void testInverseAssociation() {
        String json = "{\"[Book_Id]\": \"" +
                learningGraphQLId1 +
                "\", \"`Author_Id`\": \"" +
                danId +
                "\"}";
        Tuple2<Long, Long> idPair = new BinLogParser().initialize(sqlClient)
                .parseIdPair(
                        AuthorProps.BOOKS,
                        json
                );
        Assertions.assertEquals(
                "Tuple2(_1=" +
                        danId +
                        ", _2=" +
                        learningGraphQLId1 +
                        ")",
                idPair.toString()
        );
    }
}
