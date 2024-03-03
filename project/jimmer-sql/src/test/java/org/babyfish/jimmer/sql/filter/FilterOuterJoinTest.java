package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.hr.DepartmentFetcher;
import org.babyfish.jimmer.sql.model.hr.DepartmentTable;
import org.junit.jupiter.api.Test;

public class FilterOuterJoinTest extends AbstractQueryTest {

    @Test
    public void testQuery() {
        DepartmentTable table = DepartmentTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.asTableEx().employees(JoinType.LEFT).name().eq("Jessica"))
                        .select(
                                table.fetch(
                                        DepartmentFetcher.$
                                                .name()
                                                .employeeCount()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, " +
                                    "tb_1_.NAME, " +
                                    "(select count(*) from employee where department_id = tb_1_.id) " +
                                    "from DEPARTMENT tb_1_ " +
                                    "left join EMPLOYEE tb_2_ " +
                                    "--->on tb_1_.ID = tb_2_.DEPARTMENT_ID " +
                                    "--->and tb_2_.DELETED_UUID is null " +
                                    "where " +
                                    "--->tb_2_.NAME = ? " +
                                    "--->and " +
                                    "--->tb_1_.DELETED_TIME is null"
                    );
                    ctx.rows(
                            "[{\"id\":\"1\",\"name\":\"Market\",\"employeeCount\":2}]"
                    );
                }
        );
    }
}
