package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.arrays.ArrayModelDraft;
import org.babyfish.jimmer.sql.model.arrays.ArrayModel;
import org.babyfish.jimmer.sql.model.arrays.ArrayModelProps;
import org.babyfish.jimmer.sql.model.arrays.EnumArrayModel;
import org.babyfish.jimmer.sql.model.arrays.EnumArrayModelDraft;
import org.babyfish.jimmer.sql.model.arrays.Priority;
import org.babyfish.jimmer.sql.model.pg.PgArrayModel;
import org.babyfish.jimmer.sql.model.pg.PgArrayModelDraft;
import org.babyfish.jimmer.sql.model.pg.PgArrayModelTable;
import org.babyfish.jimmer.sql.model.pg.PgEnumArrayModel;
import org.babyfish.jimmer.sql.model.pg.PgEnumArrayModelDraft;
import org.babyfish.jimmer.sql.model.pg.PgEnumArrayModelTable;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.h2.value.ValueJson;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class SaveArraysTest extends AbstractMutationTest {

    @Test
    public void testInsert() {
        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        executeAndExpectResult(
            getSqlClient().getEntities().saveCommand(
                ArrayModelDraft.$.produce(model -> {
                    model.setId(newId);
                    model.setStrings(new String[]{"1", "2", "3"});
                    model.setBytes(new Byte[]{1, 2, 3});
                    model.setInts(new int[] {4, 5, 6});
                    model.setIntegers(new Integer[]{1, 2, 3});
                    model.setLongs(new Long[]{1L, 2L, 3L});
                    model.setFloats(new Float[]{1f, 2f, 3f});
                    model.setUuids(new UUID[]{newId});
                    model.setSerializedArr(new Byte[] {1, 2, 3, 4, 5});
                })
            ).setMode(SaveMode.INSERT_ONLY),
            ctx -> {
                ctx.statement(it -> {
                    it.sql(
                        "insert into ARRAY_MODEL(ID, STRINGS, BYTES, INTS, INTEGERS, LONGS, UUIDS, FLOATS, SERIALIZED_ARR) " +
                        "values(?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                    it.variables(
                        newId,
                        new String[]{"1", "2", "3"},
                        new Byte[]{1, 2, 3},
                        new Integer[]{4, 5, 6},
                        new Integer[]{1, 2, 3},
                        new Long[]{1L, 2L, 3L},
                        new UUID[]{newId},
                        new Float[]{1f, 2f, 3f},
                        new DbLiteral.DbValue(
                                ArrayModelProps.SERIALIZED_ARR.unwrap(),
                                ValueJson.fromJson("[1, 2, 3, 4, 5]"),
                                true
                        )
                    );
                });
                ctx.entity(it -> {
                    it.original(
                            "{" +
                                    "--->\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                    "--->\"strings\":[\"1\",\"2\",\"3\"]," +
                                    "--->\"bytes\":[1,2,3]," +
                                    "--->\"ints\":[4,5,6]," +
                                    "--->\"integers\":[1,2,3]," +
                                    "--->\"longs\":[1,2,3]," +
                                    "--->\"uuids\":[\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"]," +
                                    "--->\"floats\":[1.0,2.0,3.0]," +
                                    "--->\"serializedArr\":[1,2,3,4,5]}"
                    );
                    it.modified(
                            "{" +
                                    "\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                    "\"strings\":[\"1\",\"2\",\"3\"]," +
                                    "\"bytes\":[1,2,3]," +
                                    "\"ints\":[4,5,6]," +
                                    "\"integers\":[1,2,3]," +
                                    "\"longs\":[1,2,3]," +
                                    "\"uuids\":[\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"]," +
                                    "\"floats\":[1.0,2.0,3.0]," +
                                    "\"serializedArr\":[1,2,3,4,5]}"
                    );
                });
                ctx.totalRowCount(1);
                ctx.rowCount(AffectedTable.of(ArrayModel.class), 1);
            }
        );
    }

    @Test
    public void testInsertByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        PgArrayModel model = PgArrayModelDraft.$.produce(it -> {
            it.setId(2L);
            it.setIntArr(new int[] {1, 2, 3});
            it.setIntegerArr(new Integer[] {1, 2, 3});
            it.setTextArr(new String[] {"a", "b", "c"});
            it.setTextList(Arrays.asList("d", "e", "f"));
            it.setVarcharArr(new String[] {"g", "h", "i"});
            it.setVarcharList(Arrays.asList("j", "k", "l"));
        });
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .getEntities()
                        .saveCommand(model)
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into PG_ARRAY_MODEL(ID, INT_ARR, INTEGER_ARR, TEXT_ARR, TEXT_LIST, VARCHAR_ARR, VARCHAR_LIST) " +
                                "values(?, ?, ?, ?, ?, ?, ?)");
                    });
                    ctx.entity(it -> {
                        String json = "{" +
                                "\"id\":2," +
                                "\"intArr\":[1,2,3]," +
                                "\"integerArr\":[1,2,3]," +
                                "\"textArr\":[\"a\",\"b\",\"c\"]," +
                                "\"textList\":[\"d\",\"e\",\"f\"]," +
                                "\"varcharArr\":[\"g\",\"h\",\"i\"]," +
                                "\"varcharList\":[\"j\",\"k\",\"l\"]}";
                        it.original(json);
                        it.modified(json);
                    });
                }
        );
    }

    @Test
    public void testUpdateByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        PgArrayModel model = PgArrayModelDraft.$.produce(it -> {
            it.setId(1L);
            it.setTextArr(new String[] {"A", "B", "C"});
            it.setTextList(Arrays.asList("D", "E", "F"));
            it.setVarcharArr(new String[] {"G", "H", "I"});
            it.setVarcharList(Arrays.asList("J", "K", "L"));
        });
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .getEntities()
                        .saveCommand(model)
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update PG_ARRAY_MODEL " +
                                        "set TEXT_ARR = ?, TEXT_LIST = ?, VARCHAR_ARR = ?, VARCHAR_LIST = ? " +
                                        "where ID = ?"
                        );
                    });
                    ctx.entity(it -> {
                        String json = "{" +
                                "\"id\":1," +
                                "\"textArr\":[\"A\",\"B\",\"C\"]," +
                                "\"textList\":[\"D\",\"E\",\"F\"]," +
                                "\"varcharArr\":[\"G\",\"H\",\"I\"]," +
                                "\"varcharList\":[\"J\",\"K\",\"L\"]" +
                                "}";
                        it.original(json);
                        it.modified(json);
                    });
                }
        );
    }

    @Test
    public void testUpdateDMLByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        PgArrayModelTable table = PgArrayModelTable.$;

        executeAndExpectRowCount(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .createUpdate(table)
                        .set(table.intArr(), new int[] {1, 2, 3})
                        .set(table.textArr(), new String[] {"A", "B", "C"})
                        .set(table.textList(), Arrays.asList("D", "E", "F"))
                        .set(table.varcharArr(), new String[] {"G", "H", "I"})
                        .set(table.varcharList(), Arrays.asList("J", "K", "L"))
                        .where(table.id().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update PG_ARRAY_MODEL tb_1_ set " +
                                        "INT_ARR = ?, TEXT_ARR = ?, TEXT_LIST = ?, VARCHAR_ARR = ?, VARCHAR_LIST = ? " +
                                        "where tb_1_.ID = ?"
                        );
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testInsertEnumArrays() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        EnumArrayModelDraft.$.produce(model -> {
                            model.setId(2L);
                            model.setGenders(new Gender[] {Gender.FEMALE, Gender.MALE});
                            model.setGenderList(Collections.singletonList(Gender.MALE));
                            model.setPriorities(Arrays.asList(Priority.MEDIUM, Priority.HIGH));
                        })
                ).setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ENUM_ARRAY_MODEL(ID, GENDERS, GENDER_LIST, PRIORITIES) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables(
                                2L,
                                new String[] {"F", "M"},
                                new String[] {"M"},
                                new Integer[] {1, 2}
                        );
                    });
                    ctx.entity(it -> {
                        String json = "{" +
                                "\"id\":2," +
                                "\"genders\":[\"FEMALE\",\"MALE\"]," +
                                "\"genderList\":[\"MALE\"]," +
                                "\"priorities\":[\"MEDIUM\",\"HIGH\"]" +
                                "}";
                        it.original(json);
                        it.modified(json);
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(EnumArrayModel.class), 1);
                }
        );
    }

    @Test
    public void testInsertEnumArraysByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        PgEnumArrayModel model = PgEnumArrayModelDraft.$.produce(it -> {
            it.setId(2L);
            it.setGenderArr(new Gender[] {Gender.FEMALE, Gender.MALE});
            it.setTextList(Collections.singletonList(Gender.MALE));
            it.setVarcharList(Collections.singletonList(Gender.FEMALE));
            it.setPriorities(Arrays.asList(Priority.MEDIUM, Priority.HIGH));
        });
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .getEntities()
                        .saveCommand(model)
                        .setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into PG_ENUM_ARRAY_MODEL(ID, GENDER_ARR, TEXT_LIST, VARCHAR_LIST, PRIORITIES) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.entity(it -> {
                        String json = "{" +
                                "\"id\":2," +
                                "\"genderArr\":[\"FEMALE\",\"MALE\"]," +
                                "\"textList\":[\"MALE\"]," +
                                "\"varcharList\":[\"FEMALE\"]," +
                                "\"priorities\":[\"MEDIUM\",\"HIGH\"]" +
                                "}";
                        it.original(json);
                        it.modified(json);
                    });
                }
        );
    }

    @Test
    public void testUpdateDMLEnumArraysByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        PgEnumArrayModelTable table = PgEnumArrayModelTable.$;

        executeAndExpectRowCount(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .createUpdate(table)
                        .set(table.genderArr(), new Gender[] {Gender.FEMALE})
                        .set(table.textList(), Arrays.asList(Gender.FEMALE, Gender.MALE))
                        .set(table.priorities(), Collections.singletonList(Priority.LOW))
                        .where(table.id().eq(1L))
                        .where(table.varcharList().eq(Collections.singletonList(Gender.MALE))),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update PG_ENUM_ARRAY_MODEL tb_1_ set " +
                                        "GENDER_ARR = ?, TEXT_LIST = ?, PRIORITIES = ? " +
                                        "where tb_1_.ID = ? and tb_1_.VARCHAR_LIST = ?"
                        );
                    });
                    ctx.rowCount(1);
                }
        );
    }
}
