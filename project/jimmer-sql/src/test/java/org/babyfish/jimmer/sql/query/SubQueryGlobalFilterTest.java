package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentProps;
import org.babyfish.jimmer.sql.model.hr.DepartmentTable;
import org.babyfish.jimmer.sql.model.hr.EmployeeTable;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SubQueryGlobalFilterTest extends AbstractQueryTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testSelectionJoin(boolean usedInWhere) {
        connectAndExpect(con -> {
            deleteDepartment(con);
            return getLambdaClient().createQuery(EmployeeTable.class, (q, employee) -> q
                    .where(employee.id().eq(1L))
                    .select(getLambdaClient().createSubQuery(q, EmployeeTable.class, (sq, target) -> sq
                            .where(target.id().eq(employee.id()))
                            .whereIf(usedInWhere, target.department(JoinType.LEFT).name().isNotNull())
                            .select(target.department(JoinType.LEFT).name())
                    ))
            ).execute(con);
        }, ctx -> {
            ctx.sql("select (select tb_3_.NAME from EMPLOYEE tb_2_ " +
                    "left join DEPARTMENT tb_3_ on tb_2_.DEPARTMENT_ID = tb_3_.ID and tb_3_.DELETED_MILLIS = ? " +
                    "where tb_2_.ID = tb_1_.ID " +
                    (usedInWhere ? "and tb_3_.NAME is not null " : "") +
                    "and tb_2_.DELETED_MILLIS = ?) " +
                    "from EMPLOYEE tb_1_ where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?");
            ctx.variables(0L, 0L, 1L, 0L);
            ctx.rows("[null]");
        });
    }

    @ParameterizedTest
    @CsvSource({"DEFAULT, LEFT", "DEFAULT, INNER", "REVERSED, LEFT", "REVERSED, INNER", "IGNORED, LEFT", "IGNORED, INNER"})
    public void testSelectionJoinBehavior(LogicalDeletedBehavior behavior, JoinType joinType) {
        JSqlClient client = getSqlClient().filters(it -> it.setBehavior(Department.class, behavior));
        EmployeeTable employee = EmployeeTable.$;
        boolean ignored = behavior == LogicalDeletedBehavior.IGNORED;
        boolean leftJoin = joinType == JoinType.LEFT;
        String departmentFilter = ignored ? "" :
                " and tb_3_.DELETED_MILLIS " + (behavior == LogicalDeletedBehavior.DEFAULT ? "=" : "<>") + " ?";
        connectAndExpect(con -> {
            deleteDepartment(con);
            return client.createQuery(employee).where(employee.id().eq(1L))
                    .select(client.createSubQuery(employee).where(employee.id().eq(1L))
                            .select(employee.department(joinType).name().coalesce("hidden")))
                    .execute(con);
        }, ctx -> {
            ctx.sql("select (select coalesce(tb_3_.NAME, ?) from EMPLOYEE tb_2_ " +
                    (leftJoin ? "left" : "inner") +
                    " join DEPARTMENT tb_3_ on tb_2_.DEPARTMENT_ID = tb_3_.ID" +
                    (leftJoin ? departmentFilter : "") +
                    " where tb_2_.ID = ? and tb_2_.DELETED_MILLIS = ?" + (leftJoin ? "" : departmentFilter) + ") " +
                    "from EMPLOYEE tb_1_ where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?");
            ctx.variables(ignored ? new Object[]{"hidden", 1L, 0L, 1L, 0L} : leftJoin ?
                    new Object[]{"hidden", 0L, 1L, 0L, 1L, 0L} : new Object[]{"hidden", 1L, 0L, 0L, 1L, 0L});
            ctx.rows(behavior != LogicalDeletedBehavior.DEFAULT ? "[\"Market\"]" :
                    joinType == JoinType.LEFT ? "[\"hidden\"]" : "[null]");
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testNestedSelection(boolean inWhere) {
        connectAndExpect(con -> {
            deleteDepartment(con);
            return getLambdaClient().createQuery(EmployeeTable.class, (q, employee) -> {
                ConfigurableSubQuery<Long> subQuery = getLambdaClient().createSubQuery(q, EmployeeTable.class, (sq, target) -> sq
                        .where(target.id().eq(employee.id()))
                        .select(getLambdaClient().createSubQuery(sq, DepartmentTable.class, (nested, department) -> nested
                                .where(department.id().eq(target.id()))
                                .select(department.id().count()))));
                q.where(employee.id().eq(1L));
                if (inWhere) {
                    return q.where(subQuery.eq(0L)).select(employee.id());
                }
                return q.select(subQuery);
            }).execute(con);
        }, ctx -> {
            String subQuerySql = "(select (select count(tb_3_.ID) from DEPARTMENT tb_3_ " +
                    "where tb_3_.ID = tb_2_.ID and tb_3_.DELETED_MILLIS = ?) from EMPLOYEE tb_2_ " +
                    "where tb_2_.ID = tb_1_.ID and tb_2_.DELETED_MILLIS = ?)";
            ctx.sql(inWhere ?
                    "select tb_1_.ID from EMPLOYEE tb_1_ where tb_1_.ID = ? and " + subQuerySql +
                            " = ? and tb_1_.DELETED_MILLIS = ?" :
                    "select " + subQuerySql + " from EMPLOYEE tb_1_ where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?");
            ctx.variables(inWhere ? new Object[]{1L, 0L, 0L, 0L, 0L} : new Object[]{0L, 0L, 1L, 0L});
            ctx.rows(inWhere ? "[1]" : "[0]");
        });
    }

    @Test
    public void testSharedSubQueryInSelectAndWhere() {
        EmployeeTable employee = EmployeeTable.$;
        ConfigurableSubQuery<String> subQuery = getSqlClient().createSubQuery(employee)
                .where(employee.id().eq(1L))
                .select(employee.department(JoinType.LEFT).name());
        executeAndExpect(getSqlClient().createQuery(employee)
                .where(employee.id().eq(1L), subQuery.eq("Market"))
                .select(subQuery), ctx -> {
            String subQuerySql = "(select tb_3_.NAME from EMPLOYEE tb_2_ left join DEPARTMENT tb_3_ " +
                    "on tb_2_.DEPARTMENT_ID = tb_3_.ID and tb_3_.DELETED_MILLIS = ? " +
                    "where tb_2_.ID = ? and tb_2_.DELETED_MILLIS = ?)";
            ctx.sql("select " + subQuerySql + " from EMPLOYEE tb_1_ where tb_1_.ID = ? and " +
                    subQuerySql + " = ? and tb_1_.DELETED_MILLIS = ?");
            ctx.variables(0L, 1L, 0L, 1L, 0L, 1L, 0L, "Market", 0L);
            ctx.rows("[\"Market\"]");
        });
    }

    @Test
    public void testUserFilterOnSelectionJoin() {
        JSqlClient client = getSqlClient(it -> it.addFilters(new Filter<DepartmentProps>() {
            @Override
            public void filter(FilterArgs<DepartmentProps> args) {
                args.where(args.getTable().name().ne("Market"));
            }
        }));
        EmployeeTable employee = EmployeeTable.$;
        executeAndExpect(client.createQuery(employee).where(employee.id().eq(1L))
                .select(client.createSubQuery(employee).where(employee.id().eq(1L))
                        .select(employee.department(JoinType.LEFT).name())), ctx -> {
            ctx.sql("select (select tb_3_.NAME from EMPLOYEE tb_2_ left join DEPARTMENT tb_3_ " +
                    "on tb_2_.DEPARTMENT_ID = tb_3_.ID and tb_3_.DELETED_MILLIS = ? and tb_3_.NAME <> ? " +
                    "where tb_2_.ID = ? and tb_2_.DELETED_MILLIS = ?) " +
                    "from EMPLOYEE tb_1_ where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?");
            ctx.variables(0L, "Market", 1L, 0L, 1L, 0L);
            ctx.rows("[null]");
        });
    }

    @Test
    public void testMergedSubQuerySelections() {
        EmployeeTable employee = EmployeeTable.$;
        DepartmentTable department = DepartmentTable.$;
        connectAndExpect(con -> {
            deleteDepartment(con);
            return getSqlClient().createQuery(department)
                    .where(department.name().in(
                            getSqlClient().createSubQuery(employee).where(employee.id().eq(1L))
                                    .select(employee.department().name())
                                    .unionAll(getSqlClient().createSubQuery(employee).where(employee.id().eq(2L))
                                            .select(employee.department().name()))))
                    .select(department.id()).execute(con);
        }, ctx -> {
            ctx.sql("select tb_1_.ID from DEPARTMENT tb_1_ where tb_1_.NAME in ((" +
                    "select tb_3_.NAME from EMPLOYEE tb_2_ inner join DEPARTMENT tb_3_ on tb_2_.DEPARTMENT_ID = tb_3_.ID " +
                    "where tb_2_.ID = ? and tb_2_.DELETED_MILLIS = ? and tb_3_.DELETED_MILLIS = ?) union all (" +
                    "select tb_5_.NAME from EMPLOYEE tb_4_ inner join DEPARTMENT tb_5_ on tb_4_.DEPARTMENT_ID = tb_5_.ID " +
                    "where tb_4_.ID = ? and tb_4_.DELETED_MILLIS = ? and tb_5_.DELETED_MILLIS = ?)) " +
                    "and tb_1_.DELETED_MILLIS = ?");
            ctx.variables(1L, 0L, 0L, 2L, 0L, 0L, 0L);
            ctx.rows("[]");
        });
    }

    @Test
    public void testExistsDiscardsUnusedSelectionJoin() {
        EmployeeTable employee = EmployeeTable.$;
        executeAndExpect(getSqlClient().createQuery(employee).where(employee.id().eq(1L))
                .where(getSqlClient().createSubQuery(employee).where(employee.id().eq(1L))
                        .select(employee.department().name()).exists())
                .select(employee.id()), ctx -> {
            ctx.sql("select tb_1_.ID from EMPLOYEE tb_1_ where tb_1_.ID = ? and exists(" +
                    "select 1 from EMPLOYEE tb_2_ where tb_2_.ID = ? and tb_2_.DELETED_MILLIS = ?) " +
                    "and tb_1_.DELETED_MILLIS = ?");
            ctx.variables(1L, 1L, 0L, 0L);
            ctx.rows("[1]");
        });
    }

    private static void deleteDepartment(Connection con) {
        try (Statement statement = con.createStatement()) {
            statement.executeUpdate("update DEPARTMENT set DELETED_MILLIS = 9 where ID = 1");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
