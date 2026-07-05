package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.exception.SaveErrorCode;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationDraft;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationProps;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinedInheritanceOptimisticLockTest extends AbstractMutationTest {

    @Test
    public void testUserOptimisticLockUsesFakeRootGate() {
        connectAndExpect(
                con -> {
                    getSqlClient()
                            .getEntities()
                            .saveCommand(
                                    OrganizationDraft.$.produce(organization -> {
                                        organization.setId(200L);
                                        organization.setTaxCode("GLOBEX-LOCKED");
                                    })
                            )
                            .setMode(SaveMode.UPDATE_ONLY)
                            .setOptimisticLock(
                                    OrganizationTable.class,
                                    (table, it) -> table.name().eq("Globex")
                            )
                            .execute(con);
                    return joinedClientRow(con, 200L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and NAME = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(200L, "Globex", "ORG");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_ORGANIZATION " +
                                        "set TAX_CODE = ? " +
                                        "where ID = ? and exists(" +
                                        "select 1 from JOINED_CLIENT tb_root_ " +
                                        "where tb_root_.ID = ? and tb_root_.CLIENT_TYPE = ?)"
                        );
                        it.variables("GLOBEX-LOCKED", 200L, 200L, "ORG");
                    });
                    ctx.value("[ORG, Globex, GLOBEX-LOCKED, null, null]");
                }
        );
    }

    @Test
    public void testUserOptimisticLockFailureSkipsJoinedChildUpdate() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(200L);
                                    organization.setTaxCode("SHOULD-NOT-WRITE");
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setOptimisticLock(
                                OrganizationTable.class,
                                (table, it) -> table.name().eq("Wrong")
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update JOINED_CLIENT " +
                                        "set /* fake update to return all ids */ ID = ID " +
                                        "where ID = ? and NAME = ? and CLIENT_TYPE = ?"
                        );
                        it.variables(200L, "Wrong", "ORG");
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
    public void testUserOptimisticLockRejectsDerivedTypeProp() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(200L);
                                    organization.setTaxCode("SHOULD-NOT-WRITE");
                                })
                        )
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setOptimisticLock(
                                OrganizationTable.class,
                                (table, it) -> table.taxCode().eq(it.newString(OrganizationProps.TAX_CODE))
                        ),
                ctx -> {
                    ctx.throwable(it -> {
                        it.type(IllegalArgumentException.class);
                        it.message(
                                "User optimistic lock predicate for joined inheritance type " +
                                        "\"org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client\" " +
                                        "can only reference root-table properties, but " +
                                        "\"org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization.taxCode\" " +
                                        "is not declared by the inheritance root or its mapped superclasses"
                        );
                    });
                }
        );
    }

    private static String joinedClientRow(Connection con, long id) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select c.CLIENT_TYPE, c.NAME, o.TAX_CODE, p.FIRST_NAME, p.LAST_NAME " +
                        "from JOINED_CLIENT c " +
                        "left join JOINED_ORGANIZATION o on c.ID = o.ID " +
                        "left join JOINED_PERSON p on c.ID = p.ID " +
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
                        rs.getString(3) +
                        ", " +
                        rs.getString(4) +
                        ", " +
                        rs.getString(5) +
                        "]";
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
