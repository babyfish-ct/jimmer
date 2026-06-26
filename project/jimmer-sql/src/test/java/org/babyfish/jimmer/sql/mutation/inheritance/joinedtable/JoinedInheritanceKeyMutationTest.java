package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.key.KeyOrganizationDraft;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinedInheritanceKeyMutationTest extends AbstractMutationTest {

    @Test
    public void testUpdateSubtypeByKey() {
        connectAndExpect(
                con -> {
                    getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                            .getEntities()
                            .saveCommand(
                                    KeyOrganizationDraft.$.produce(organization -> {
                                        organization.setCode("same-code");
                                        organization.setTaxCode("KEY-GLOBEX-002");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedKeyClientRow(con, "ORG", "same-code") +
                            "; " +
                            joinedKeyClientRow(con, "KeyPerson", "same-code");
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_KEY_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where CLIENT_TYPE = ? and CODE = ?"
                        );
                        it.variables("ORG", "same-code");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_KEY_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_KEY_CLIENT " +
                                        "where JOINED_KEY_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                        );
                        it.variables("KEY-GLOBEX-002", 400L, 400L, "ORG");
                    });
                    ctx.value("[400, ORG, same-code, Key Globex, KEY-GLOBEX-002, null, null]; " +
                            "[401, KeyPerson, same-code, Key Alice, null, Key Alice, Smith]");
                }
        );
    }

    @Test
    public void testUpsertSubtypeByKey() {
        connectAndExpect(
                con -> {
                    getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                            .getEntities()
                            .saveCommand(
                                    KeyOrganizationDraft.$.produce(organization -> {
                                        organization.setCode("same-code");
                                        organization.setName("Key Globex+");
                                        organization.setTaxCode("KEY-GLOBEX-003");
                                    })
                            )
                            .execute(con);
                    return joinedKeyClientRow(con, "ORG", "same-code") +
                            "; " +
                            joinedKeyClientRow(con, "KeyPerson", "same-code");
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_KEY_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(CLIENT_TYPE, CODE, NAME) " +
                                        "on tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE and tb_1_.CODE = tb_2_.CODE " +
                                        "when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE " +
                                        "then update set NAME = tb_2_.NAME " +
                                        "when not matched then insert(CLIENT_TYPE, CODE, NAME) " +
                                        "values(tb_2_.CLIENT_TYPE, tb_2_.CODE, tb_2_.NAME)"
                        );
                        it.variables("ORG", "same-code", "Key Globex+");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_KEY_ORGANIZATION(ID, TAX_CODE) " +
                                        "key(ID) values(?, ?)"
                        );
                        it.variables(400L, "KEY-GLOBEX-003");
                    });
                    ctx.value("[400, ORG, same-code, Key Globex+, KEY-GLOBEX-003, null, null]; " +
                            "[401, KeyPerson, same-code, Key Alice, null, Key Alice, Smith]");
                }
        );
    }

    @Test
    public void testInsertIfAbsentSubtypeByKeyExistingSkipsChildTable() {
        connectAndExpect(
                con -> {
                    getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                            .getEntities()
                            .saveCommand(
                                    KeyOrganizationDraft.$.produce(organization -> {
                                        organization.setCode("same-code");
                                        organization.setName("Should not update");
                                        organization.setTaxCode("SHOULD-NOT-WRITE");
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .execute(con);
                    return joinedKeyClientRow(con, "ORG", "same-code") +
                            "; " +
                            joinedKeyClientRow(con, "KeyPerson", "same-code");
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_KEY_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(CLIENT_TYPE, CODE, NAME) " +
                                        "on tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE and tb_1_.CODE = tb_2_.CODE " +
                                        "when not matched then insert(CLIENT_TYPE, CODE, NAME) " +
                                        "values(tb_2_.CLIENT_TYPE, tb_2_.CODE, tb_2_.NAME)"
                        );
                        it.variables("ORG", "same-code", "Should not update");
                    });
                    ctx.value("[400, ORG, same-code, Key Globex, KEY-GLOBEX-001, null, null]; " +
                            "[401, KeyPerson, same-code, Key Alice, null, Key Alice, Smith]");
                }
        );
    }

    @Test
    public void testInsertIfAbsentSubtypeByKeyMissingInsertsChildTable() {
        connectAndExpect(
                con -> {
                    getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                            .getEntities()
                            .saveCommand(
                                    KeyOrganizationDraft.$.produce(organization -> {
                                        organization.setCode("new-key-code");
                                        organization.setName("New Key Org");
                                        organization.setTaxCode("NEW-KEY-001");
                                    })
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .execute(con);
                    return joinedKeyClientRow(con, "ORG", "new-key-code");
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into JOINED_KEY_CLIENT tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(CLIENT_TYPE, CODE, NAME) " +
                                        "on tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE and tb_1_.CODE = tb_2_.CODE " +
                                        "when not matched then insert(CLIENT_TYPE, CODE, NAME) " +
                                        "values(tb_2_.CLIENT_TYPE, tb_2_.CODE, tb_2_.NAME)"
                        );
                        it.variables("ORG", "new-key-code", "New Key Org");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into JOINED_KEY_ORGANIZATION(ID, TAX_CODE) " +
                                        "values(?, ?)"
                        );
                        it.variables(UNKNOWN_VARIABLE, "NEW-KEY-001");
                    });
                    ctx.value("[UNKNOWN, ORG, new-key-code, New Key Org, NEW-KEY-001, null, null]");
                }
        );
    }

    private static String joinedKeyClientRow(Connection con, String type, String code) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select c.ID, c.CLIENT_TYPE, c.CODE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                        "from JOINED_KEY_CLIENT c " +
                        "left join JOINED_KEY_ORGANIZATION o on c.ID = o.ID " +
                        "left join JOINED_KEY_PERSON p on c.ID = p.ID " +
                        "where c.CLIENT_TYPE = ? and c.CODE = ?"
        )) {
            stmt.setString(1, type);
            stmt.setString(2, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return "[" +
                        (rs.getLong(1) >= 400L && rs.getLong(1) <= 401L ? rs.getLong(1) : "UNKNOWN") +
                        ", " +
                        rs.getString(2) +
                        ", " +
                        rs.getString(3) +
                        ", " +
                        rs.getString(4) +
                        ", " +
                        rs.getString(5) +
                        ", " +
                        rs.getString(6) +
                        ", " +
                        rs.getString(7) +
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
