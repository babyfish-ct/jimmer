package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.json.MedicineDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MatchByKeyTest extends AbstractMutationTest {

    @Test
    public void testNoKeyGroup() {
        executeAndExpectResult(
                getSqlClient().saveCommand(MedicineDraft.$.produce(draft -> draft.setId(1))).matchByKey(),
                ctx -> ctx.throwable(it -> it.type(IllegalArgumentException.class))
        );
    }

    @Test
    public void testMultipleGroupsEvenWhenOnlyOneIsLoaded() {
        SysUser input = SysUserDraft.$.produce(draft -> {
            draft.setId(100);
            draft.setAccount("sysusr_001");
        });
        executeAndExpectResult(
                getSqlClient().saveCommand(input).matchByKey(),
                ctx -> ctx.throwable(it -> it.type(IllegalArgumentException.class))
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testMissingKeysNeverFallBackToId(boolean inferKeys) {
        Book idOnly = BookDraft.$.produce(draft -> draft.setId(Constants.graphQLInActionId3));
        Book partialKey = BookDraft.$.produce(idOnly, draft -> draft.setName("GraphQL in Action"));
        for (Book input : Arrays.asList(idOnly, partialKey)) {
            SimpleEntitySaveCommand<Book> command = getSqlClient().saveCommand(input);
            executeAndExpectResult(
                    inferKeys ? command.matchByKey() : command.setKeyProps(BookProps.NAME, BookProps.EDITION).matchByKey(),
                    ctx -> ctx.throwable(it -> it.type(IllegalArgumentException.class))
            );
        }
    }

    @Test
    public void testBatchIsValidatedBeforeWriting() {
        Book valid = BookDraft.$.produce(draft -> {
            draft.setId(UUID.randomUUID());
            draft.setName("New book");
            draft.setEdition(1);
            draft.setPrice(BigDecimal.ONE);
        });
        Book invalid = BookDraft.$.produce(draft -> draft.setId(Constants.graphQLInActionId3));
        executeAndExpectResult(
                getSqlClient().saveEntitiesCommand(Arrays.asList(valid, invalid)).matchByKey(),
                ctx -> ctx.throwable(it -> it.type(IllegalArgumentException.class))
        );
    }

    @Test
    public void testSingleNamedGroup() {
        NamedKeyUser input = NamedKeyUserDraft.$.produce(draft -> {
            draft.setId(100);
            draft.setAccount("sysusr_001");
            draft.setDescription("Updated by named key");
        });
        jdbc(con -> {
            try (java.sql.Statement statement = con.createStatement()) {
                statement.execute("create local temporary table named_key_user(" +
                        "id bigint primary key, account varchar unique, description varchar)");
                statement.execute("insert into named_key_user(id, account) values(1, 'sysusr_001')");
            }
            SimpleSaveResult<NamedKeyUser> result = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .saveCommand(input).matchByKey().setMode(SaveMode.UPDATE_ONLY).execute(con);
            assertTrue(result.isAccepted());
            assertEquals(1L, result.getModifiedEntity().id());
            assertEquals("Updated by named key", getSqlClient().getEntities().forConnection(con)
                    .findById(NamedKeyUser.class, 1L).description());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testExplicitKeysTakePrecedenceRegardlessOfCallOrder(boolean matchFirst) {
        SysUser input = SysUserDraft.$.produce(draft -> {
            draft.setId(100);
            draft.setEmail("tom.cook@gmail.com");
            draft.setDescription("Updated by explicit key");
        });
        SimpleEntitySaveCommand<SysUser> command = getSqlClient(it -> it.setDialect(new H2Dialect())).saveCommand(input);
        command = matchFirst ?
                command.matchByKey().setKeyProps("2", SysUserProps.EMAIL) :
                command.setKeyProps("2", SysUserProps.EMAIL).matchByKey();
        SimpleEntitySaveCommand<SysUser> configured = command.setMode(SaveMode.UPDATE_ONLY);
        jdbc(con -> {
            SimpleSaveResult<SysUser> result = configured.execute(con);
            assertTrue(result.isAccepted());
            assertEquals(1L, result.getModifiedEntity().id());
            assertEquals(1, result.getTotalAffectedRowCount());
        });
    }

    @Test
    public void testEmptyBatch() {
        executeAndExpectResult(
                getSqlClient().saveEntitiesCommand(Collections.<Book>emptyList()).matchByKey(),
                ctx -> ctx.totalRowCount(0)
        );
    }

    @Test
    public void testCommandConfigurationDoesNotMutateOriginal() {
        Book input = BookDraft.$.produce(draft -> {
            draft.setId(Constants.graphQLInActionId3);
            draft.setName("Renamed book");
            draft.setEdition(3);
            draft.setPrice(BigDecimal.ONE);
        });
        SimpleEntitySaveCommand<Book> byId = getSqlClient(it -> it.setDialect(new H2Dialect()))
                .saveCommand(input).setMode(SaveMode.UPDATE_ONLY);
        SimpleEntitySaveCommand<Book> byKey = byId.matchByKey();
        jdbc(con -> {
            assertFalse(byKey.execute(con).isAccepted());
            assertTrue(byId.execute(con).isAccepted());
            assertEquals("Renamed book", getSqlClient().getEntities().forConnection(con)
                    .findById(Book.class, input.id()).name());
        });
    }
}
