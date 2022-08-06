package org.babyfish.jimmer.sql.binlog;

import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.binlog.BinLogParser;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class BinLogTest {

    @Test
    public void test() {
        String json = "{\"Node_Id\": 2, \"[Name]\": 3, \"`Parent_Id`\": 1}";
        TreeNode treeNode = new BinLogParser().parseEntity(TreeNode.class, json);
        Assertions.assertEquals(
                "{\"id\":2,\"name\":\"3\",\"parent\":{\"id\":1}}",
                treeNode.toString()
        );
    }

    @Test
    public void testAssociation() {
        String json = "{\"[Book_Id]\": \"" +
                learningGraphQLId1 +
                "\", \"`Author_Id`\": \"" +
                danId +
                "\"}";
        Tuple2<Long, Long> idPair = new BinLogParser()
                .parseIdPair(
                        ImmutableProps.join(BookTableEx.class, BookTableEx::authors),
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
        Tuple2<Long, Long> idPair = new BinLogParser()
                .parseIdPair(
                        ImmutableProps.join(AuthorTableEx.class, AuthorTableEx::books),
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
