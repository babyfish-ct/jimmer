package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.model.flat.ProvinceProps;
import org.junit.jupiter.api.Test;

public class MixChildOperationTest extends AbstractChildOperatorTest {

    @Test
    public void testDisconnectTreeExcept() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(),
                            con,
                            ProvinceProps.COUNTRY.unwrap()
                    ).disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2),
                                    new Tuple2<>(2L, 4)
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update COMPANY set STREET_ID = null " +
                                        "where STREET_ID in (" +
                                        "--->select ID from STREET where CITY_ID in (" +
                                        "--->--->select ID from CITY where PROVINCE_ID in (" +
                                        "--->--->--->select ID " +
                                        "--->--->--->from PROVINCE " +
                                        "--->--->--->where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))" +
                                        "--->--->)" +
                                        "--->)" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from STREET where CITY_ID in (" +
                                        "--->select ID from CITY where PROVINCE_ID in (" +
                                        "--->--->select ID " +
                                        "--->--->from PROVINCE " +
                                        "--->--->where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))" +
                                        "--->)" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from CITY where PROVINCE_ID in (" +
                                        "--->select ID " +
                                        "--->from PROVINCE " +
                                        "--->where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from PROVINCE " +
                                        "where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))"
                        );
                    });
                }
        );
    }

    @Test
    public void testDisconnectTreeWithShallowSubQueryDepthExcept() {
        connectAndExpect(
                con -> {
                    return operator(
                            getSqlClient(it -> it.setMaxMutationSubQueryDepth(2)),
                            con,
                            ProvinceProps.COUNTRY.unwrap()
                    ).disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2L),
                                    new Tuple2<>(2L, 4L)
                            )
                    );
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from STREET tb_1_ " +
                                        "where tb_1_.CITY_ID in (" +
                                        "--->select tb_2_.ID " +
                                        "--->from CITY tb_2_ " +
                                        "--->where tb_2_.PROVINCE_ID in (" +
                                        "--->--->select tb_3_.ID " +
                                        "--->--->from PROVINCE tb_3_ " +
                                        "--->--->where " +
                                        "--->--->--->tb_3_.COUNTRY_ID in (?, ?) " +
                                        "--->--->and " +
                                        "--->--->--->(tb_3_.COUNTRY_ID, tb_3_.ID) not in ((?, ?), (?, ?))" +
                                        "--->)" +
                                        ")"
                        );
                        it.variables(1L, 2L, 1L, 2L, 2L, 4L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from CITY where PROVINCE_ID in (" +
                                        "--->select ID " +
                                        "--->from PROVINCE " +
                                        "--->where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                        it.variables(1L, 2L, 1L, 2L, 2L, 4L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from PROVINCE " +
                                        "where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))"
                        );
                        it.variables(1L, 2L, 1L, 2L, 2L, 4L);
                    });
                }
        );
    }
}
