package org.babyfish.jimmer.sql.mutation;

import com.mysql.cj.MysqlConnection;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Statement;
import java.util.Arrays;

/**
 * @see IdentityTest
 */
public class GetIdTest extends AbstractMutationTest {

    public void testGetIdFromH2() {

        resetIdentity(null);

        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new H2Dialect()));
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Market");
            draft.addIntoEmployees(emp -> {
                emp.setName("Jacob");
            });
            draft.addIntoEmployees(emp -> {
                emp.setName("Jessica");
            });
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Sales");
            draft.addIntoEmployees(emp -> {
                emp.setName("Oakes");
            });
        });

        executeAndExpectResult(
                sqlClient.getEntities().saveEntitiesCommand(
                        Arrays.asList(department1, department2)
                ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("");
                    });
                }
        );
    }

    private void resetIdentity(DataSource dataSource) {
        jdbc(dataSource, false, con -> {
            String suffix = con instanceof MysqlConnection ?
                    "auto_increment = 100" :
                    "alter id restart with 100";
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(
                        "alter table department " + suffix
                );
            }
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(
                        "alter table employee " + suffix
                );
            }
        });
    }
}
