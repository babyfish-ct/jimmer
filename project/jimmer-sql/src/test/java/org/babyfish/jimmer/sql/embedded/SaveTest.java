package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.embedded.Machine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class SaveTest extends AbstractMutationTest {

    @Test
    public void testUpdate() {
        Machine machine = Immutables.createMachine(draft -> {
            draft.location(true).setHost("localhost");
            draft.location(true).setPort(8080);
            draft.setCpuFrequency(3);
            draft.setMemorySize(16);
            draft.setDiskSize(512);
        });
        setAutoIds(Machine.class, 1L);
        executeAndExpectResult(
                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .getEntities()
                        .saveCommand(machine),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into MACHINE(" +
                                        "--->HOST, PORT, CPU_FREQUENCY, MEMORY_SIZE, DISK_SIZE" +
                                        ") key(HOST, PORT) values(" +
                                        "--->?, ?, ?, ?, ?" +
                                        ")"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "\"location\":{\"host\":\"localhost\",\"port\":8080}," +
                                        "\"cpuFrequency\":3," +
                                        "\"memorySize\":16," +
                                        "\"diskSize\":512" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "\"id\":1," +
                                        "\"location\":{\"host\":\"localhost\",\"port\":8080}," +
                                        "\"cpuFrequency\":3," +
                                        "\"memorySize\":16," +
                                        "\"diskSize\":512}"
                        );
                    });
                    ctx.rowCount(AffectedTable.of(Machine.class), 1);
                }
        );
    }

    @Test
    public void testInsert() {
        Machine machine = Immutables.createMachine(draft -> {
           draft.location(true).setHost("server");
           draft.location(true).setPort(80);
           draft.setCpuFrequency(3);
           draft.setMemorySize(16);
           draft.setDiskSize(512);
        });
        executeAndExpectResult(
                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .getEntities()
                        .saveCommand(machine),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into MACHINE(" +
                                        "--->HOST, PORT, CPU_FREQUENCY, MEMORY_SIZE, DISK_SIZE" +
                                        ") key(HOST, PORT) values(" +
                                        "--->?, ?, ?, ?, ?" +
                                        ")"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "\"location\":{\"host\":\"server\",\"port\":80}," +
                                        "\"cpuFrequency\":3," +
                                        "\"memorySize\":16," +
                                        "\"diskSize\":512" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "\"id\":100," +
                                        "\"location\":{\"host\":\"server\",\"port\":80}," +
                                        "\"cpuFrequency\":3," +
                                        "\"memorySize\":16," +
                                        "\"diskSize\":512" +
                                        "}"
                        );
                    });
                    ctx.rowCount(AffectedTable.of(Machine.class), 1);
                }
        );
    }

//    @Test
//    public void testUpsertWithNullable() {
//        TreeNode treeNode1 = TreeNodeDraft.$.produce(draft -> {
//            draft.setName("Home");
//            draft.setParent(null);
//        });
//        TreeNode treeNode2 = TreeNodeDraft.$.produce(draft -> {
//            draft.setName("IceCream");
//            draft.setParentId(2L);
//        });
//        TreeNode treeNode3 = TreeNodeDraft.$.produce(draft -> {
//            draft.setName("Candy");
//            draft.setParentId(2L);
//        });
//        executeAndExpectResult(
//                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
//                        .getEntities()
//                        .saveEntitiesCommand(
//                                Arrays.asList(treeNode1, treeNode2, treeNode3)
//                        ),
//                ctx -> {
//                    ctx.statement(it -> {
//                        it.sql(
//                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
//                                        "from TREE_NODE tb_1_ " +
//                                        "where " +
//                                        "--->((tb_1_.NAME, tb_1_.PARENT_ID) in ((?, ?), (?, ?)) " +
//                                        "or " +
//                                        "--->tb_1_.PARENT_ID is null " +
//                                        "and " +
//                                        "--->tb_1_.NAME = ?)"
//                        );
//                        it.variables("IceCream", 2L, "Candy", 2L, "Home");
//                        it.queryReason(QueryReason.NULL_NOT_DISTINCT_REQUIRED);
//                    });
//                    ctx.statement(it -> {
//                        it.sql("insert into TREE_NODE(NAME, PARENT_ID) values(?, ?)");
//                        it.batchVariables(0, "IceCream", 2L);
//                        it.batchVariables(1, "Candy", 2L);
//                    });
//                    ctx.statement(it -> {});
//                    ctx.statement(it -> {});
//                    ctx.statement(it -> {});
//
//                }
//        );
//    }
}
