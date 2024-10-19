package org.babyfish.jimmer.sql.json;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.embedded.Machine;
import org.babyfish.jimmer.sql.model.json.Medicine;
import org.babyfish.jimmer.sql.model.json.MedicineDraft;
import org.babyfish.jimmer.sql.model.json.MedicineProps;
import org.babyfish.jimmer.sql.model.json.MedicineTable;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.h2.value.ValueJson;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class H2SaveTest extends AbstractMutationTest {

    @Test
    public void testDML() {
        MedicineTable table = MedicineTable.$;
        List<Medicine.Tag> tags = Arrays.asList(
                new Medicine.Tag("Tag-1", "Description-1"),
                new Medicine.Tag("Tag-2", "Description-2")
        );
        executeAndExpectRowCount(
                getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .createUpdate(table)
                        .set(table.tags(), tags)
                        .where(table.id().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update MEDICINE tb_1_ set TAGS = ? where tb_1_.ID = ?");
                        it.variables(
                                new DbLiteral.DbValue(
                                        MedicineProps.TAGS.unwrap(),
                                        ValueJson.fromJson("[{\"name\":\"Tag-1\",\"description\":\"Description-1\"},{\"name\":\"Tag-2\",\"description\":\"Description-2\"}]"),
                                        true
                                ),
                                1L
                        );
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testInsert() {
        connectAndExpect(con -> {
            int affectedCount = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .getEntities()
                    .saveCommand(
                            MedicineDraft.$.produce(draft -> {
                                draft.setId(10L);
                                draft.setTags(
                                        Arrays.asList(
                                                new Medicine.Tag("Tag-1", "Description-1"),
                                                new Medicine.Tag("Tag-2", "Description-2")
                                        )
                                );
                            })
                    ).setMode(SaveMode.INSERT_ONLY)
                    .execute(con)
                    .getTotalAffectedRowCount();
            Medicine medicine = getSqlClient().getEntities().forConnection(con).findById(
                    Medicine.class,
                    10L
            );
            return new Tuple2<>(affectedCount, medicine);
        }, ctx -> {
            ctx.statement(it -> {
                it.sql("insert into MEDICINE(ID, TAGS) values(?, ?)");
            });
            ctx.statement(it -> {
                it.sql(
                        "select tb_1_.ID, tb_1_.TAGS " +
                                "from MEDICINE tb_1_ " +
                                "where tb_1_.ID = ?"
                );
            });
            ctx.value(
                    "Tuple2(" +
                            "--->_1=1, " +
                            "--->_2={" +
                            "--->--->\"id\":10," +
                            "--->--->\"tags\":[" +
                            "--->--->--->{\"name\":\"Tag-1\",\"description\":\"Description-1\"}," +
                            "--->--->--->{\"name\":\"Tag-2\",\"description\":\"Description-2\"}" +
                            "--->--->]" +
                            "--->}" +
                            ")"
            );
        });
    }

    @Test
    public void testUpdate() {
        connectAndExpect(con -> {
            int affectedCount = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .getEntities()
                    .saveCommand(
                            MedicineDraft.$.produce(draft -> {
                                draft.setId(1L);
                                draft.setTags(
                                        Arrays.asList(
                                                new Medicine.Tag("Tag-1", "Description-1"),
                                                new Medicine.Tag("Tag-2", "Description-2")
                                        )
                                );
                            })
                    ).setMode(SaveMode.UPDATE_ONLY)
                    .execute(con)
                    .getTotalAffectedRowCount();
            Medicine medicine = getSqlClient().getEntities().forConnection(con).findById(
                    Medicine.class,
                    1L
            );
            return new Tuple2<>(affectedCount, medicine);
        }, ctx -> {
            ctx.statement(it -> {
                it.sql("update MEDICINE set TAGS = ? where ID = ?");
            });
            ctx.statement(it -> {
                it.sql(
                        "select tb_1_.ID, tb_1_.TAGS " +
                                "from MEDICINE tb_1_ " +
                                "where tb_1_.ID = ?"
                );
            });
            ctx.value(
                    "Tuple2(" +
                            "--->_1=1, " +
                            "--->_2={" +
                            "--->--->\"id\":1," +
                            "--->--->\"tags\":[" +
                            "--->--->--->{\"name\":\"Tag-1\",\"description\":\"Description-1\"}," +
                            "--->--->--->{\"name\":\"Tag-2\",\"description\":\"Description-2\"}" +
                            "--->--->]" +
                            "--->}" +
                            ")"
            );
        });
    }

    @Test
    public void testInsertEmbeddedJson() {
        Map<String, String> factoryMap = new LinkedHashMap<>();
        factoryMap.put("F-A", "Factory-A");
        factoryMap.put("F-B", "Factory-B");
        Map<String, String> patentMap = new LinkedHashMap<>();
        patentMap.put("P-A", "PATENT-A");
        patentMap.put("P-B", "PATENT-B");
        Machine machine = Immutables.createMachine(draft -> {
            draft.setId(10L);
            draft.setCpuFrequency(2);
            draft.setMemorySize(16);
            draft.setDiskSize(512);
            draft.applyLocation(location -> {
                location.setHost("localhost");
                location.setPort(9090);
            });
            draft.applyDetail(detail -> {
                detail.setFactories(factoryMap);
                detail.setPatents(patentMap);
            });
        });
        connectAndExpect(
                con -> {
                    int affectRowCount = getSqlClient()
                            .getEntities()
                            .forConnection(con)
                            .saveCommand(machine)
                            .setMode(SaveMode.INSERT_ONLY)
                            .execute()
                            .getTotalAffectedRowCount();
                    Machine medicine = getSqlClient().getEntities().forConnection(con).findById(
                            Machine.class,
                            10L
                    );
                    return new Tuple2<>(affectRowCount, medicine);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into MACHINE(" +
                                        "ID, " +
                                        "HOST, PORT, " +
                                        "CPU_FREQUENCY, MEMORY_SIZE, DISK_SIZE, " +
                                        "factory_map, patent_map" +
                                        ") values(?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, " +
                                        "tb_1_.HOST, tb_1_.PORT, tb_1_.SECONDARY_HOST, tb_1_.SECONDARY_PORT, " +
                                        "tb_1_.CPU_FREQUENCY, tb_1_.MEMORY_SIZE, tb_1_.DISK_SIZE, " +
                                        "tb_1_.factory_map, tb_1_.patent_map " +
                                        "from MACHINE tb_1_ where tb_1_.ID = ?"
                        );
                    });
                    ctx.value(
                            "Tuple2(" +
                                    "--->_1=1, " +
                                    "--->_2={" +
                                    "--->--->\"id\":10," +
                                    "--->--->\"location\":{\"host\":\"localhost\",\"port\":9090}," +
                                    "--->--->\"secondaryLocation\":null," +
                                    "--->--->\"cpuFrequency\":2," +
                                    "--->--->\"memorySize\":16," +
                                    "--->--->\"diskSize\":512," +
                                    "--->--->\"detail\":{" +
                                    "--->--->--->\"factories\":{\"F-A\":\"Factory-A\",\"F-B\":\"Factory-B\"}," +
                                    "--->--->--->\"patents\":{\"P-A\":\"PATENT-A\",\"P-B\":\"PATENT-B\"}" +
                                    "--->--->}" +
                                    "--->}" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testUpdateEmbeddedJson() {
        Map<String, String> patentMap = new LinkedHashMap<>();
        patentMap.put("P-A", "Patent-A");
        patentMap.put("P-B", "Patent-B");
        Machine machine = Immutables.createMachine(draft -> {
            draft.setId(1L);
            draft.setCpuFrequency(4);
            draft.setMemorySize(16);
            draft.setDiskSize(512);
            draft.applyLocation(location -> {
                location.setHost("localhost");
                location.setPort(9090);
            });
            draft.applyDetail(detail -> {
                detail.setPatents(patentMap);
            });
        });
        connectAndExpect(
                con -> {
                    int affectRowCount = getSqlClient()
                            .getEntities()
                            .forConnection(con)
                            .saveCommand(machine)
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute()
                            .getTotalAffectedRowCount();
                    Machine medicine = getSqlClient().getEntities().forConnection(con).findById(
                            Machine.class,
                            1L
                    );
                    return new Tuple2<>(affectRowCount, medicine);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update MACHINE set " +
                                        "HOST = ?, PORT = ?, " +
                                        "CPU_FREQUENCY = ?, MEMORY_SIZE = ?, DISK_SIZE = ?, " +
                                        "patent_map = ? " +
                                        "where ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, " +
                                        "tb_1_.HOST, tb_1_.PORT, tb_1_.SECONDARY_HOST, tb_1_.SECONDARY_PORT, " +
                                        "tb_1_.CPU_FREQUENCY, tb_1_.MEMORY_SIZE, tb_1_.DISK_SIZE, " +
                                        "tb_1_.factory_map, tb_1_.patent_map " +
                                        "from MACHINE tb_1_ where tb_1_.ID = ?"
                        );
                    });
                    ctx.value(
                            "Tuple2(" +
                                    "--->_1=1, " +
                                    "--->_2={" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"location\":{\"host\":\"localhost\",\"port\":9090}," +
                                    "--->--->\"secondaryLocation\":null," +
                                    "--->--->\"cpuFrequency\":4," +
                                    "--->--->\"memorySize\":16," +
                                    "--->--->\"diskSize\":512," +
                                    "--->--->\"detail\":{" +
                                    "--->--->--->\"factories\":{\"f-1\":\"factory-1\",\"f-2\":\"factory-2\"}," +
                                    "--->--->--->\"patents\":{\"P-A\":\"Patent-A\",\"P-B\":\"Patent-B\"}" +
                                    "--->--->}" +
                                    "--->}" +
                                    ")"
                    );
                }
        );
    }
}
