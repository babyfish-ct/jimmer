package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.Machine;
import org.junit.jupiter.api.Test;

public class SaveTest extends AbstractMutationTest {

    @Test
    public void testUpdate() {
        Machine machine = Objects.createMachine(draft -> {
            draft.location(true).setHost("localhost");
            draft.location(true).setPort(8080);
            draft.setCpuFrequency(3);
            draft.setMemorySize(16);
            draft.setDiskSize(512);
        });
        setAutoIds(Machine.class, 1L);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(machine),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.HOST, tb_1_.PORT " +
                                        "from MACHINE tb_1_ " +
                                        "where (tb_1_.HOST, tb_1_.PORT) = (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update MACHINE " +
                                        "set CPU_FREQUENCY = ?, MEMORY_SIZE = ?, DISK_SIZE = ? " +
                                        "where ID = ?"
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
                }
        );
    }

    @Test
    public void testInsert() {
        Machine machine = Objects.createMachine(draft -> {
           draft.location(true).setHost("server");
           draft.location(true).setPort(80);
           draft.setCpuFrequency(3);
           draft.setMemorySize(16);
           draft.setDiskSize(512);
        });
        setAutoIds(Machine.class, 100L);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(machine),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.HOST, tb_1_.PORT " +
                                        "from MACHINE tb_1_ " +
                                        "where (tb_1_.HOST, tb_1_.PORT) = (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into MACHINE(ID, HOST, PORT, CPU_FREQUENCY, MEMORY_SIZE, DISK_SIZE) " +
                                        "values(?, ?, ?, ?, ?, ?)"
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
                }
        );
    }
}
