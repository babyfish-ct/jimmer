package org.babyfish.jimmer.sql.binlog;

import org.babyfish.jimmer.sql.event.binlog.BinLogs;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BinLogTest {

    @Test
    public void test() {
        String json = "{\"Node_Id\": 2, \"[Name]\": 3, \"`Parent_Id`\": 1}";
        TreeNode treeNode = BinLogs.parse(TreeNode.class, json);
        Assertions.assertEquals(
                "{\"id\":2,\"name\":\"3\",\"parent\":{\"id\":1}}",
                treeNode.toString()
        );
    }
}
