package org.babyfish.jimmer.sql.mutation.inheritance.joinedtable;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.inverse.*;
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

    private JSqlClient triggerClient() {
        JSqlClient client = getSqlClient();
        client.getTriggers().addEntityListener(ImmutableType.get(Citizen.class), e -> {});
        client.getTriggers().addEntityListener(ImmutableType.get(Passport.class), e -> {});
        return client;
    }
}
