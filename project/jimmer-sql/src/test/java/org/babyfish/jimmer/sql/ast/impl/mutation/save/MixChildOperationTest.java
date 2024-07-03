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
}
