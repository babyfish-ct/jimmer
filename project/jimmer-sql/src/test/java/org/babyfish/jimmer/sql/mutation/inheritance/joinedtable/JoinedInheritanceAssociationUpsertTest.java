package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.upsert.ClientContactProps;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.upsert.PersonDraft;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.upsert.PersonFetcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    public void testAppendIfAbsentAssociationWithFetcher() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .getEntities()
                        .saveCommand(
                                PersonDraft.$.produce(person -> {
                                    person.setId(700L);
                                    person.setName("Upsert Person+");
                                    person.setFirstName("Upsert+");
                                    person.addIntoContacts(contact -> {
                                        contact.setNum("contact-num");
                                        contact.setTitle("Contact title");
                                    });
                                })
                        )
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                        .setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                        .execute(con, PersonFetcher.$.allScalarFields().contacts())
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, CLIENT_TYPE from final table (" +
                                        "merge into JOINED_UPSERT_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(ID, NAME, CLIENT_TYPE) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(ID, NAME, CLIENT_TYPE) " +
                                        "values(tb_2_.ID, tb_2_.NAME, tb_2_.CLIENT_TYPE)" +
                                        ")"
                        );
                        it.variables(700L, "Upsert Person+", "Person");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, LAST_NAME from final table (" +
                                        "merge into JOINED_UPSERT_PERSON tb_1_ " +
                                        "using(values(?, ?)) tb_2_(ID, FIRST_NAME) " +
                                        "on tb_1_.ID = tb_2_.ID " +
                                        "when matched then update set FIRST_NAME = tb_2_.FIRST_NAME " +
                                        "when not matched then insert(ID, FIRST_NAME) " +
                                        "values(tb_2_.ID, tb_2_.FIRST_NAME)" +
                                        ")"
                        );
                        it.variables(700L, "Upsert+");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_UPSERT_CLIENT_CONTACT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(NUM, TITLE, CLIENT_ID) " +
                                        "on tb_1_.NUM = tb_2_.NUM " +
                                        "when not matched then insert(NUM, TITLE, CLIENT_ID) " +
                                        "values(tb_2_.NUM, tb_2_.TITLE, tb_2_.CLIENT_ID)"
                        );
                        it.variables("contact-num", "Contact title", 700L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_UPSERT_CLIENT_CONTACT tb_1_ " +
                                        "where tb_1_.CLIENT_ID = ?"
                        );
                        it.variables(700L);
                    });
                    ctx.value(person -> {
                        assertEquals("Person", person.lastName());
                        assertEquals(1, person.contacts().size());
                    });
                }
        );
    }

    @Test
    public void testAppendIfAbsentAssociationWithFetcherWithoutReturning() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
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
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                        .setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                        .setSaveReturningEnabled(false)
                        .execute(con, PersonFetcher.$.contacts())
                        .getModifiedEntity(),
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
                        it.variables(700L, "Person", "Upsert Person+");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_UPSERT_PERSON(ID, FIRST_NAME, LAST_NAME) " +
                                        "key(ID) values(?, ?, ?)"
                        );
                        it.variables(700L, "Upsert+", "Person+");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_UPSERT_CLIENT_CONTACT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(NUM, TITLE, CLIENT_ID) " +
                                        "on tb_1_.NUM = tb_2_.NUM " +
                                        "when not matched then insert(NUM, TITLE, CLIENT_ID) " +
                                        "values(tb_2_.NUM, tb_2_.TITLE, tb_2_.CLIENT_ID)"
                        );
                        it.variables("contact-num", "Contact title", 700L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from JOINED_UPSERT_CLIENT_CONTACT tb_1_ " +
                                        "where tb_1_.CLIENT_ID = ?"
                        );
                        it.variables(700L);
                    });
                    ctx.value(person -> assertEquals(1, person.contacts().size()));
                }
        );
    }
}
