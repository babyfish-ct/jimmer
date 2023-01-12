package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.inheritance2.Animal;
import org.babyfish.jimmer.sql.model.inheritance2.AnimalDraft;
import org.junit.jupiter.api.Test;

public class IdInSuperMutationTest extends AbstractMutationTest {

    @Test
    public void save() {
        setAutoIds(Animal.class, 3L);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        AnimalDraft.$.produce(draft -> draft.setName("Bear"))
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.NAME from ANIMAL as tb_1_ where tb_1_.NAME = ? for update");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into ANIMAL(ID, NAME) values(?, ?)");
                    });
                    ctx.entity(it -> {
                        it.original("{\"name\":\"Bear\"}");
                        it.modified("{\"id\":3,\"name\":\"Bear\"}");
                    });
                }
        );
    }
}

