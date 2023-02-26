package org.babyfish.jimmer.example.save.common;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

public class AbstractMutationWithTriggerTest extends AbstractMutationTest {

    private final List<String> events = new ArrayList<>();

    @Override
    protected void customize(JSqlClient.Builder builder) {
        builder.setTriggerType(TriggerType.TRANSACTION_ONLY);
    }

    @BeforeEach
    public void registerEventListeners() {
        sql().getTriggers().addEntityListener(e -> {
            events.add(
                    "The entity \"" +
                            e.getImmutableType() +
                            "\" is changed, " +
                            "old: " + e.getOldEntity() +
                            ", new: " + e.getNewEntity()
            );
        });
        sql().getTriggers().addAssociationListener(e -> {
            events.add(
                    "The association \"" +
                            e.getImmutableProp() +
                            "\" is changed, " +
                            "source id: " + e.getSourceId() +
                            ", detached target id: " + e.getDetachedTargetId() +
                            ", attached target id: " + e.getAttachedTargetId()
            );
        });
    }

    protected void assertEvents(String ... events) {
        int size = Math.min(this.events.size(), events.length);
        for (int i = 0; i < size; i++) {
            Assertions.assertEquals(
                    events[i],
                    this.events.get(i),
                    "events[i]: expected \"" +
                            events[i] +
                            "\", actual \"" +
                            this.events.get(i) +
                            "\""
            );
        }
        Assertions.assertEquals(
                events.length,
                this.events.size(),
                "Event count: expected " +
                        events.length +
                        ", actual " +
                        this.events.size()
        );
    }
}
