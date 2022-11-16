package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.RedirectedProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
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

    protected void makeEventAssertions() {
        Map<String, String> map = new HashMap<>();
        for (Field field : Constants.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                UUID uuid;
                try {
                    uuid = (UUID) field.get(null);
                } catch (IllegalAccessException ex) {
                    throw new AssertionError(ex);
                }
                map.put(uuid.toString(), "\" + " + field.getName() + " + \"");
            }
        }
        map.put("56506a3c-801b-4f7d-a41d-e889cdc3d67d", "\" + newId + \"");
        StringBuilder builder = new StringBuilder();
        int indent = 0;
        for (String event : events) {
            builder.append('"');
            char prev = '\0';
            for (int i = 0; i < event.length(); i++) {
                char c = event.charAt(i);
                switch (c) {
                    case '{':
                        builder.append("{\" + \n");
                        ++indent;
                        builder.append("        \"");
                        for (int j = indent; j > 0; --j) {
                            builder.append("--->");
                        }
                        break;
                    case '}':
                        builder.append("\" + \n");
                        builder.append("        \"");
                        --indent;
                        for (int j = indent; j > 0; --j) {
                            builder.append("--->");
                        }
                        builder.append('}');
                        break;
                    case ',':
                        if (indent > 1) {
                            builder.append(",\" + \n");
                        } else {
                            builder.append(", \" + \n");
                        }
                        builder.append("        \"");
                        for (int j = indent; j > 0; --j) {
                            builder.append("--->");
                        }
                        break;
                    case ' ':
                        if (prev != ',') {
                            builder.append(' ');
                        }
                        break;
                    case '"':
                        builder.append("\\\"");
                        break;
                    default:
                        builder.append(c);
                }
                prev = c;
            }
            builder.append("\",\n");
        }
        String text = builder.toString();
        for (Map.Entry<String, String> e : map.entrySet()) {
            text = text.replace(e.getKey(), e.getValue());
        }
        System.out.println(text);
    }
}
