package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.hr.EmployeeTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InsertFromSelectLogicalDeleteTest extends AbstractMutationTest {

    @Test
    public void testNonBooleanLogicalDeleteColumnBelongsToConflictKey() {
        JSqlClient sqlClient = getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE));
        EmployeeTable sourceEmployee = EmployeeTable.$;
        BaseTable2<StringExpression, ComparableExpression<Gender>> source = sqlClient
                .createBaseQuery(sourceEmployee)
                .where(sourceEmployee.id().eq(2L))
                .addSelect(sourceEmployee.name())
                .addSelect(sourceEmployee.gender())
                .asBaseTable();
        EmployeeTable table = EmployeeTable.$;
        connectAndExpect(
                con -> sqlClient
                        .createUpsert(table, source)
                        .key(table.name(), source.get_1())
                        .merge(table.gender(), source.get_2())
                        .returning(table.id(), table.gender(), table.deletedMillis())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, GENDER, DELETED_MILLIS from final table (" +
                                        "merge into EMPLOYEE tb_2_ using (" +
                                        "select tb_1_.NAME c1, tb_1_.GENDER c2 from EMPLOYEE tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?" +
                                        ") tb_1_ on tb_2_.NAME = tb_1_.c1 " +
                                        "and tb_2_.DELETED_MILLIS = ? " +
                                        "when matched then update set tb_2_.GENDER = tb_1_.c2 " +
                                        "when not matched then insert(NAME, GENDER, DELETED_MILLIS) " +
                                        "values(tb_1_.c1, tb_1_.c2, ?)" +
                                        ")"
                        );
                        it.variables(2L, 0L, 0L, 0L);
                    });
                    ctx.value(rows -> {
                        assertEquals(1, rows.size());
                        assertEquals(2L, rows.get(0).get_1());
                        assertEquals(Gender.FEMALE, rows.get(0).get_2());
                        assertEquals(0L, rows.get(0).get_3());
                    });
                }
        );
    }
}
