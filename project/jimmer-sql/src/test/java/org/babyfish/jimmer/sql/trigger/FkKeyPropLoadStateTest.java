package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.Endorsement;
import org.babyfish.jimmer.sql.model.EndorsementDraft;
import org.babyfish.jimmer.sql.model.EndorsementFetcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.babyfish.jimmer.sql.common.Constants.eveId;
import static org.babyfish.jimmer.sql.common.Constants.oreillyId;

/**
 * Reproduces a production bug: an entity with two real-FK `@Key` props referencing
 * two distinct parent entity types, and its own client-side-generated id, loses the
 * loaded-state of one FK prop on the `EntityEvent.getNewEntity()` fired for an INSERT
 * via a TRANSACTION_ONLY trigger.
 */
public class FkKeyPropLoadStateTest extends AbstractMutationTest {

    @Test
    public void testBothRealFkKeyPropsLoadedOnInsertEvent() {
        JSqlClient sqlClient = getSqlClient(builder -> builder.setTriggerType(TriggerType.TRANSACTION_ONLY));

        AtomicReference<EntityEvent<Endorsement>> capturedEvent = new AtomicReference<>();
        sqlClient.getTriggers(true).addEntityListener(Endorsement.class, capturedEvent::set);

        jdbc(con ->
                sqlClient.getEntities().saveCommand(
                        EndorsementDraft.$.produce(draft -> {
                            draft.setCode("GOLD_TIER");
                            draft.applyBookStore(s -> s.setId(oreillyId));
                            draft.applyAuthor(a -> a.setId(eveId));
                            draft.setLevel("FEATURED");
                        })
                ).execute(
                        con,
                        EndorsementFetcher.$.level().bookStore()
                )
        );

        EntityEvent<Endorsement> event = capturedEvent.get();
        Assertions.assertNotNull(event, "No EntityEvent was fired for the insert");
        assertRealFkKeyPropsLoaded(event);
    }

    @Test
    public void testBothRealFkKeyPropsLoadedOnUpdateEvent() {
        JSqlClient sqlClient = getSqlClient(builder -> builder.setTriggerType(TriggerType.TRANSACTION_ONLY));

        AtomicReference<UUID> endorsementId = new AtomicReference<>();
        AtomicReference<EntityEvent<Endorsement>> capturedEvent = new AtomicReference<>();
        sqlClient.getTriggers(true).addEntityListener(Endorsement.class, capturedEvent::set);

        jdbc(con -> {
            SimpleSaveResult<Endorsement> result = sqlClient.getEntities().saveCommand(
                    EndorsementDraft.$.produce(draft -> {
                        draft.setCode("GOLD_TIER_FOR_UPDATE");
                        draft.applyBookStore(s -> s.setId(oreillyId));
                        draft.applyAuthor(a -> a.setId(eveId));
                        draft.setLevel("FEATURED");
                    })
            ).execute(con);
            endorsementId.set(result.getModifiedEntity().id());

            capturedEvent.set(null);

            sqlClient.getEntities().saveCommand(
                    EndorsementDraft.$.produce(draft -> {
                        draft.setId(endorsementId.get());
                        draft.setCode("GOLD_TIER_FOR_UPDATE");
                        draft.applyBookStore(s -> s.setId(oreillyId));
                        draft.applyAuthor(a -> a.setId(eveId));
                        draft.setLevel("PLATINUM");
                    })
            ).setMode(SaveMode.UPDATE_ONLY).execute(
                    con,
                    EndorsementFetcher.$.level().bookStore()
            );
        });

        EntityEvent<Endorsement> event = capturedEvent.get();
        Assertions.assertNotNull(event, "No EntityEvent was fired for the update");
        assertRealFkKeyPropsLoaded(event);
    }

    private static void assertRealFkKeyPropsLoaded(EntityEvent<Endorsement> event) {
        ImmutableSpi newEntity = (ImmutableSpi) event.getNewEntity();
        ImmutableType type = newEntity.__type();

        Assertions.assertTrue(
                newEntity.__isLoaded(type.getProp("bookStore").getId()),
                "`bookStore` (real-FK @Key prop) should still be loaded on the fired EntityEvent's newEntity, " +
                        "because it was explicitly set on the input draft before save"
        );
        Assertions.assertTrue(
                newEntity.__isLoaded(type.getProp("author").getId()),
                "`author` (real-FK @Key prop) should still be loaded on the fired EntityEvent's newEntity, " +
                        "because it was explicitly set on the input draft before save"
        );
    }
}
