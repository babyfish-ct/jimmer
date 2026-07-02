package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.exception.SaveErrorCode;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade.OrganizationDraft;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade.PersonDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinedInheritanceCascadeOptimisticLockTest extends AbstractMutationTest {

    @Test
    public void testRootVersionAcceptsJoinedChildUpdate() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(500L);
                                        organization.setVersion(0);
                                        organization.setTaxCode("C-GLOBEX-LOCKED");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .execute(con);
                    return joinedCascadeClientRow(con, 500L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CASCADE_CLIENT " +
                                        "set VERSION = VERSION + 1 " +
                                        "where ID = ? and VERSION = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(500L, 0, "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CASCADE_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CASCADE_CLIENT " +
                                        "where JOINED_CASCADE_CLIENT.ID = ? and CLIENT_TYPE = ?)"
                        );
                        it.variables("C-GLOBEX-LOCKED", 500L, 500L, "ORG");
                    });
                    ctx.value("[ORG, Cascade Globex, 1, C-GLOBEX-LOCKED, null, null]");
                }
        );
    }

    @Test
    public void testRootVersionDerivedTypeMismatchIsOptimisticLockError() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(501L);
                                    organization.setVersion(0);
                                    organization.setTaxCode("SHOULD-NOT-WRITE");
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CASCADE_CLIENT " +
                                        "set VERSION = VERSION + 1 " +
                                        "where ID = ? and VERSION = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(501L, 0, "ORG");
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.OptimisticLockError.class);
                        it.detail(ex -> Assertions.assertEquals(
                                SaveErrorCode.OPTIMISTIC_LOCK_ERROR,
                                ((SaveException) ex).getSaveErrorCode()
                        ));
                    });
                }
        );
    }

    @Test
    public void testRootVersionFailureStopsTypeChangeCleanup() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                PersonDraft.$.produce(person -> {
                                    person.setId(500L);
                                    person.setVersion(9);
                                    person.setFirstName("Should");
                                    person.setLastName("Not Write");
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setTypeChangeAllowed(true),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CASCADE_CLIENT where ID = ? order by ID");
                        it.variables(500L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CASCADE_CLIENT " +
                                        "set CLIENT_TYPE = ?, VERSION = VERSION + 1 " +
                                        "where ID = ? and VERSION = ? and CLIENT_TYPE = ?"
                        );
                        it.variables("Person", 500L, 9, "ORG");
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.OptimisticLockError.class);
                        it.detail(ex -> Assertions.assertEquals(
                                SaveErrorCode.OPTIMISTIC_LOCK_ERROR,
                                ((SaveException) ex).getSaveErrorCode()
                        ));
                    });
                }
        );
    }

    @Test
    public void testRootVersionMissingTypeChangeIsOptimisticLockError() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                PersonDraft.$.produce(person -> {
                                    person.setId(599L);
                                    person.setVersion(0);
                                    person.setFirstName("Missing");
                                    person.setLastName("Person");
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setTypeChangeAllowed(true),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID, CLIENT_TYPE from JOINED_CASCADE_CLIENT where ID = ? order by ID");
                        it.variables(599L);
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.OptimisticLockError.class);
                        it.detail(ex -> Assertions.assertEquals(
                                SaveErrorCode.OPTIMISTIC_LOCK_ERROR,
                                ((SaveException) ex).getSaveErrorCode()
                        ));
                    });
                }
        );
    }

    private static String joinedCascadeClientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select c.CLIENT_TYPE, c.NAME, c.VERSION, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                        "from JOINED_CASCADE_CLIENT c " +
                        "left join JOINED_CASCADE_ORGANIZATION o on c.ID = o.ID " +
                        "left join JOINED_CASCADE_PERSON p on c.ID = p.ID " +
                        "where c.ID = ?"
        )) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return "[" +
                        rs.getString(1) +
                        ", " +
                        rs.getString(2) +
                        ", " +
                        rs.getInt(3) +
                        ", " +
                        rs.getString(4) +
                        ", " +
                        rs.getString(5) +
                        ", " +
                        rs.getString(6) +
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
