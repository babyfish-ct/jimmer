package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.hr.EmployeeTable;
import org.babyfish.jimmer.sql.model.hr.dto.EmployeeSpecificationForIssue735;
import org.junit.jupiter.api.Test;

public class EmployeeSpecificationTest extends AbstractQueryTest {

    @Test
    public void testParentEmpty() {
        EmployeeTable table = EmployeeTable.$;
        EmployeeSpecificationForIssue735 spec = new EmployeeSpecificationForIssue735();
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(spec)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.GENDER, tb_1_.DELETED_MILLIS, tb_1_.DEPARTMENT_ID " +
                                    "from EMPLOYEE tb_1_ " +
                                    "where tb_1_.DELETED_MILLIS = ?"
                    );
                    ctx.rows(it -> {});
                }
        );
    }

    @Test
    public void testParentIsNull() {
        EmployeeTable table = EmployeeTable.$;
        EmployeeSpecificationForIssue735 spec = new EmployeeSpecificationForIssue735();
        spec.setDepartmentNull(true);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(spec)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.GENDER, tb_1_.DELETED_MILLIS, tb_1_.DEPARTMENT_ID " +
                                    "from EMPLOYEE tb_1_ " +
                                    "where tb_1_.DEPARTMENT_ID is null and tb_1_.DELETED_MILLIS = ?"
                    );
                    ctx.rows(it -> {});
                }
        );
    }

    @Test
    public void testParentIsNullAndDepartmentName() {
        EmployeeTable table = EmployeeTable.$;
        EmployeeSpecificationForIssue735 spec = new EmployeeSpecificationForIssue735();
        spec.setDepartmentNull(true);
        spec.setDepartmentName("X");
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(spec)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.GENDER, tb_1_.DELETED_MILLIS, tb_1_.DEPARTMENT_ID " +
                                    "from EMPLOYEE tb_1_ " +
                                    "inner join DEPARTMENT tb_2_ on " +
                                    "--->tb_1_.DEPARTMENT_ID = tb_2_.ID and " +
                                    "--->tb_2_.DELETED_MILLIS = ? " +
                                    "where " +
                                    "--->tb_1_.DEPARTMENT_ID is null and " +
                                    "--->tb_2_.NAME = ? and " +
                                    "--->tb_1_.DELETED_MILLIS = ?"
                    );
                    ctx.rows(it -> {});
                }
        );
    }
}
