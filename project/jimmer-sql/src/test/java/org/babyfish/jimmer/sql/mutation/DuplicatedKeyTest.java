package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class DuplicatedKeyTest extends AbstractMutationTest {

    @Test
    public void testKey() {
        List<Department> departments = Arrays.asList(
                Immutables.createDepartment(draft -> draft.setName("Develop")),
                Immutables.createDepartment(draft -> draft.setName("Develop"))
        );
        executeAndExpectResult(
                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .saveEntitiesCommand(departments),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into DEPARTMENT(NAME, DELETED_MILLIS) " +
                                        "key(NAME, DELETED_MILLIS) values(?, ?)"
                        );
                        it.variables("Develop", 0L);
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":\"100\",\"name\":\"Develop\"}");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":\"100\",\"name\":\"Develop\"}");
                    });
                }
        );
    }
}
