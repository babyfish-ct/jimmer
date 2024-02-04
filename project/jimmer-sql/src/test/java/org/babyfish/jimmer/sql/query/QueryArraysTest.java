package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.arrays.ArrayModel;
import org.babyfish.jimmer.sql.model.arrays.ArrayModelTable;
import org.babyfish.jimmer.sql.model.pg.PgArrayModel;
import org.babyfish.jimmer.sql.model.pg.PgArrayModelTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

public class QueryArraysTest extends AbstractQueryTest {

    @Test
    public void testQueryArrayProperties() {
        connectAndExpect(
            con -> {
                return getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .findById(ArrayModel.class, Constants.arrayModelId);
            },
            ctx -> {
                ctx.sql(
                  "select tb_1_.ID, tb_1_.STRINGS, tb_1_.BYTES, tb_1_.INTS, tb_1_.INTEGERS, tb_1_.LONGS, tb_1_.UUIDS, tb_1_.FLOATS " +
                          "from ARRAY_MODEL tb_1_ " +
                          "where tb_1_.ID = ?"
                );
                ctx.rows(
                  "[" +
                      "--->{" +
                      "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db635\"," +
                      "--->--->\"strings\":[\"3\",\"2\",\"1\"]," +
                      "--->--->\"bytes\":[3,2,1]," +
                      "--->--->\"ints\":[6,5,4]," +
                      "--->--->\"integers\":[3,2,1]," +
                      "--->--->\"longs\":[3,2,1]," +
                      "--->--->\"uuids\":[\"e110c564-23cc-4811-9e81-d587a13db635\"]," +
                      "--->--->\"floats\":[3.0,2.0,1.0]" +
                      "--->}" +
                      "]"
                );
        });
    }

    @Test
    public void testQueryArrayPropertiesWithConditions() {
        ArrayModelTable table = ArrayModelTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.strings().eq(new String[] {"3", "2", "1"}))
                        .where(table.ints().eq(new int[] {6, 5, 4}))
                        .where(table.integers().eq(new Integer[] {3, 2, 1}))
                        .where(table.uuids().eq(new UUID[] { UUID.fromString("e110c564-23cc-4811-9e81-d587a13db635") }))
                        .where(table.floats().eq(new Float[] {3F, 2F, 1F}))
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.STRINGS, tb_1_.BYTES, tb_1_.INTS, tb_1_.INTEGERS, tb_1_.LONGS, tb_1_.UUIDS, tb_1_.FLOATS " +
                                    "from ARRAY_MODEL tb_1_ " +
                                    "where tb_1_.STRINGS = ? and tb_1_.INTS = ? and tb_1_.INTEGERS = ? and tb_1_.UUIDS = ? and tb_1_.FLOATS = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db635\"," +
                                    "--->--->\"strings\":[\"3\",\"2\",\"1\"]," +
                                    "--->--->\"bytes\":[3,2,1]," +
                                    "--->--->\"ints\":[6,5,4]," +
                                    "--->--->\"integers\":[3,2,1]," +
                                    "--->--->\"longs\":[3,2,1]," +
                                    "--->--->\"uuids\":[\"e110c564-23cc-4811-9e81-d587a13db635\"]," +
                                    "--->--->\"floats\":[3.0,2.0,1.0]" +
                                    "--->}" +
                                    "]"
                    );
                });
    }

    @Test
    public void testQueryArrayPropertiesByPostgres() {
        NativeDatabases.assumeNativeDatabase();
        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> {
                    return getSqlClient(it -> it.setDialect(new PostgresDialect()))
                            .getEntities()
                            .forConnection(con)
                            .findById(PgArrayModel.class, 1L);
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.INT_ARR, tb_1_.INTEGER_ARR, " +
                                    "tb_1_.TEXT_ARR, tb_1_.TEXT_LIST, " +
                                    "tb_1_.VARCHAR_ARR, tb_1_.VARCHAR_LIST " +
                                    "from PG_ARRAY_MODEL tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":1," +
                                    "--->\"intArr\":[1,2,3]," +
                                    "--->\"integerArr\":[4,5,6]," +
                                    "--->\"textArr\":[\"a\",\"b\",\"c\"]," +
                                    "--->\"textList\":[\"d\",\"e\",\"f\"]," +
                                    "--->\"varcharArr\":[\"g\",\"h\",\"i\"]," +
                                    "--->\"varcharList\":[\"j\",\"k\",\"l\"]" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testQueryArrayPropertiesWithConditionsByPostgres() {
        NativeDatabases.assumeNativeDatabase();
        PgArrayModelTable table = PgArrayModelTable.$;
        executeAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .createQuery(table)
                        .where(table.intArr().eq(new int[] {1, 2, 3}))
                        .where(table.integerArr().eq(new Integer[] {4, 5, 6}))
                        .where(table.textArr().eq(new String[] { "a", "b", "c" }))
                        .where(table.textList().eq(Arrays.asList("d", "e", "f")))
                        .where(table.varcharArr().eq(new String[] {"g", "h", "i"}))
                        .where(table.varcharList().eq(Arrays.asList("j", "k", "l")))
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.INT_ARR, tb_1_.INTEGER_ARR, " +
                                    "tb_1_.TEXT_ARR, tb_1_.TEXT_LIST, " +
                                    "tb_1_.VARCHAR_ARR, tb_1_.VARCHAR_LIST " +
                                    "from PG_ARRAY_MODEL tb_1_ " +
                                    "where " +
                                    "tb_1_.INT_ARR = ? and tb_1_.INTEGER_ARR = ? and " +
                                    "tb_1_.TEXT_ARR = ? and tb_1_.TEXT_LIST = ? and " +
                                    "tb_1_.VARCHAR_ARR = ? and tb_1_.VARCHAR_LIST = ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":1," +
                                    "--->\"intArr\":[1,2,3]," +
                                    "--->\"integerArr\":[4,5,6]," +
                                    "--->\"textArr\":[\"a\",\"b\",\"c\"]," +
                                    "--->\"textList\":[\"d\",\"e\",\"f\"]," +
                                    "--->\"varcharArr\":[\"g\",\"h\",\"i\"]," +
                                    "--->\"varcharList\":[\"j\",\"k\",\"l\"]" +
                                    "}]"
                    );
                }
        );
    }
}
