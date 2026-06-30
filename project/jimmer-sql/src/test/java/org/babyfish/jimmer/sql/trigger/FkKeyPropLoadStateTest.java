package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.Endorsement;
import org.babyfish.jimmer.sql.model.EndorsementDraft;
import org.babyfish.jimmer.sql.model.EndorsementFetcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
