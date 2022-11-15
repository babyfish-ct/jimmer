package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.RedirectedProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AbstractTriggerTest extends AbstractMutationTest {

    private List<String> events = new ArrayList<>();

    private boolean eventAsserted = false;

    @BeforeEach
    public void initializeTriggerTest() {
        events.clear();
        eventAsserted = false;
    }

    @AfterEach
    public void terminateTriggerTest() {
        if (!eventAsserted) {
            Assertions.fail("`assertEvents` has not been called");
        }
    }

    @Override
    protected JSqlClient getSqlClient(Consumer<JSqlClient.Builder> block) {
        JSqlClient sqlClient = super.getSqlClient(builder -> {
            block.accept(builder);
            builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
        });
        EntityManager em = new EntityManager(
                AbstractTriggerTest.class.getClassLoader(),
                Book.class.getPackage().getName()
        );
        for (ImmutableType type : em.getAllTypes()) {
            if (type.isEntity()) {
                sqlClient.getTriggers(true).addEntityListener(type, e -> events.add(e.toString()));
                for (ImmutableProp prop : type.getProps().values()) {
                    if (prop.isAssociation(TargetLevel.ENTITY)) {
                        sqlClient.getTriggers(true).addAssociationListener(
                                RedirectedProp.source(prop, type),
                                e -> events.add(e.toString())
                        );
                    }
                }
            }
        }
        return sqlClient;
    }

    protected void assertEvents(String ... events) {
        eventAsserted = true;
        int len = Math.min(events.length, this.events.size());
        for (int i = 0; i < len; i++) {
            String expected = events[i].replace("--->", "");
            Assertions.assertEquals(expected, this.events.get(i), "events[" + i + "]");
        }
        Assertions.assertEquals(events.length, this.events.size(), "event count");
    }
}
