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
                                "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?)) tb_2_(NAME, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when matched then " +
                                        "--->update set /* fake update to return all ids */ DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when not matched then " +
                                        "--->insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)"
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
