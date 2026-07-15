package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.inverse.*;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.junit.jupiter.api.Test;

public class JoinedInheritanceInverseAssociationTest extends AbstractQueryTest {

    @Test
    public void testFetchInverseOneToOneDeclaredInSubType() {
        CitizenTable table = CitizenTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(700L))
                        .select(
                                table.fetch(
                                        CitizenFetcher.$
                                                .name()
                                                .passport(PassportFetcher.$.name())
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from JOINED_CITIZEN tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(700L);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from JOINED_DOCUMENT tb_1_ " +
                                    "inner join JOINED_PASSPORT tb_1__sub " +
                                    "on tb_1_.ID = tb_1__sub.ID " +
                                    "where tb_1__sub.CITIZEN_ID = ? " +
                                    "and tb_1_.DOCUMENT_TYPE = ?"
                    );
                    ctx.statement(1).variables(700L, "PASSPORT");
                    ctx.rows(
                            "[{\"id\":700,\"name\":\"PassportHolder\"," +
                                    "\"passport\":{\"id\":701,\"name\":\"primary-passport\"}}]"
                    );
                }
        );
    }

    @Test
    public void testBackRefIdsOnInverseSideEvict() {
        // TriggersImpl.fireEvictEvents -> BackRefIds.findBackRefIds, branch
        // "backProp without mappedBy": Passport.citizen points to the changed Citizen
        JSqlClient client = triggerClient();
        connectAndExpect(
                con -> {
                    client.getTriggers().fireEntityEvict(
                            ImmutableType.get(Citizen.class),
                            700L,
                            con
                    );
                    return null;
                },
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.ID " +
                                    "from JOINED_DOCUMENT tb_1_ " +
                                    "inner join JOINED_PASSPORT tb_1__sub " +
                                    "on tb_1_.ID = tb_1__sub.ID " +
                                    "where tb_1__sub.CITIZEN_ID = ? " +
                                    "and tb_1_.DOCUMENT_TYPE = ?"
                    ).variables(700L, "PASSPORT");
                }
        );
    }

    @Test
    public void testBackRefIdsOnOwningSideEvict() {
        // TriggersImpl.fireEvictEvents -> BackRefIds.findBackRefIds, branch
        // "backProp with column-definition mappedBy": Citizen.passport mappedBy Passport.citizen
        JSqlClient client = triggerClient();
        connectAndExpect(
                con -> {
                    client.getTriggers().fireEntityEvict(
                            ImmutableType.get(Passport.class),
                            701L,
                            con
                    );
                    return null;
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1__sub.CITIZEN_ID " +
                                    "from JOINED_DOCUMENT tb_1_ " +
                                    "inner join JOINED_PASSPORT tb_1__sub " +
                                    "on tb_1_.ID = tb_1__sub.ID " +
                                    "where tb_1_.ID = ? " +
                                    "and tb_1__sub.CITIZEN_ID is not null " +
                                    "and tb_1_.DOCUMENT_TYPE = ?"
                    ).variables(701L, "PASSPORT");
                }
        );
    }

    @Test
    public void testForwardJoinThroughSubTypeForeignKey() {
        PassportTable table = PassportTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.citizen().name().eq("PassportHolder"))
                        .select(table.name(), table.citizen().name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NAME, tb_2_.NAME " +
                                    "from JOINED_DOCUMENT tb_1_ " +
                                    "inner join JOINED_PASSPORT tb_1__sub " +
                                    "on tb_1_.ID = tb_1__sub.ID " +
                                    "inner join JOINED_CITIZEN tb_2_ " +
                                    "on tb_1__sub.CITIZEN_ID = tb_2_.ID " +
                                    "where tb_2_.NAME = ? " +
                                    "and tb_1_.DOCUMENT_TYPE = ?"
                    ).variables("PassportHolder", "PASSPORT");
                    ctx.rows(
                            "[{\"_1\":\"primary-passport\",\"_2\":\"PassportHolder\"}]"
                    );
                }
        );
    }

    @Test
    public void testSelectSubTypeForeignKeyId() {
        PassportTable table = PassportTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(701L))
                        .select(table.citizenId()),
                ctx -> {
                    ctx.sql(
                            "select tb_1__sub.CITIZEN_ID " +
                                    "from JOINED_DOCUMENT tb_1_ " +
                                    "inner join JOINED_PASSPORT tb_1__sub " +
                                    "on tb_1_.ID = tb_1__sub.ID " +
                                    "where tb_1_.ID = ? " +
                                    "and tb_1_.DOCUMENT_TYPE = ?"
                    ).variables(701L, "PASSPORT");
                    ctx.rows("[700]");
                }
        );
    }

    @Test
    public void testForwardJoinThroughJoinedSubTypeTarget() {
        PassportLinkTable table = PassportLinkTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.passport().citizen().name().eq("PassportHolder"))
                        .select(
                                table.id(),
                                table.passport().name(),
                                table.passport().citizen().name()
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_2__document.NAME, tb_3_.NAME " +
                                    "from JOINED_PASSPORT_LINK tb_1_ " +
                                    "inner join JOINED_PASSPORT tb_2_ " +
                                    "on tb_1_.PASSPORT_ID = tb_2_.ID " +
                                    "inner join JOINED_DOCUMENT tb_2__document " +
                                    "on tb_2_.ID = tb_2__document.ID " +
                                    "inner join JOINED_CITIZEN tb_3_ " +
                                    "on tb_2_.CITIZEN_ID = tb_3_.ID " +
                                    "where tb_3_.NAME = ?"
                    ).variables("PassportHolder");
                    ctx.rows(
                            "[{\"_1\":702,\"_2\":\"primary-passport\"," +
                                    "\"_3\":\"PassportHolder\"}]"
                    );
                }
        );
    }

    @Test
    public void testInheritedPropertyThroughLeftJoinedSubTypeTarget() {
        PassportLinkTable table = PassportLinkTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(702L))
                        .select(table.id(), table.passport(JoinType.LEFT).name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_2__document.NAME " +
                                    "from JOINED_PASSPORT_LINK tb_1_ " +
                                    "left join JOINED_PASSPORT tb_2_ " +
                                    "on tb_1_.PASSPORT_ID = tb_2_.ID " +
                                    "left join JOINED_DOCUMENT tb_2__document " +
                                    "on tb_2_.ID = tb_2__document.ID " +
                                    "where tb_1_.ID = ?"
                    ).variables(702L);
                    ctx.rows("[{\"_1\":702,\"_2\":\"primary-passport\"}]");
                }
        );
    }

    private JSqlClient triggerClient() {
        JSqlClient client = getSqlClient(
                it -> it.setEntityManager(new EntityManager(Citizen.class, Passport.class))
        );
        client.getTriggers().addEntityListener(ImmutableType.get(Citizen.class), e -> {});
        client.getTriggers().addEntityListener(ImmutableType.get(Passport.class), e -> {});
        return client;
    }
}
