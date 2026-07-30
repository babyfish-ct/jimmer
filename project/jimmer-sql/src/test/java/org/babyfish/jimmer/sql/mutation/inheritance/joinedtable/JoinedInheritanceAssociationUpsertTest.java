package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.upsert.ClientContactProps;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.upsert.PersonDraft;
import org.junit.jupiter.api.Test;

public class JoinedInheritanceAssociationUpsertTest extends AbstractMutationTest {

    @Test
    public void testMergeKeyAssociationWithUpsertMask() {
        executeAndExpectResult(
                getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .getEntities()
                        .saveCommand(
                                PersonDraft.$.produce(person -> {
                                    person.setId(700L);
                                    person.setName("Upsert Person+");
                                    person.setFirstName("Upsert+");
                                    person.setLastName("Person+");
                                    person.addIntoContacts(contact -> {
                                        contact.setNum("contact-num");
                                        contact.setTitle("Contact title");
                                    });
                                })
                        )
                        .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                        .setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                        .setUpsertMask(ClientContactProps.NUM),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_UPSERT_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, CLIENT_TYPE, NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, CLIENT_TYPE, NAME) " +
                                        "values(tb_2_.ID, tb_2_.CLIENT_TYPE, tb_2_.NAME)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_UPSERT_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_UPSERT_CLIENT_CONTACT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(NUM, TITLE, CLIENT_ID) " +
                                        "on tb_1_.NUM = tb_2_.NUM " +
                                        "when matched then update set " +
                                        "/* fake update to return all ids */ TITLE = tb_1_.TITLE " +
                                        "when not matched then insert(NUM, TITLE, CLIENT_ID) " +
                                        "values(tb_2_.NUM, tb_2_.TITLE, tb_2_.CLIENT_ID)"
                        );
                    });
                    ctx.entity(it -> {
                    });
                }
        );
    }
}
