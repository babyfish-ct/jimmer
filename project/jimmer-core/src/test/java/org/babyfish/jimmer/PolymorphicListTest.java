package org.babyfish.jimmer;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.model.ListState;
import org.babyfish.jimmer.model.ListStateDraft;
import org.babyfish.jimmer.model.TreeNodeDraft;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PolymorphicListTest {

    @Test
    public void defaultTypingRestoresRootAndNestedLists() throws Exception {
        JsonCodec<?> codec = polymorphicCodec();
        for (boolean randomAccess : Arrays.asList(true, false)) {
            for (boolean empty : Arrays.asList(true, false)) {
                List<String> input = randomAccess ? new ArrayList<>() : new LinkedList<>();
                if (!empty) {
                    input.add("a");
                }
                ListState original = ListStateDraft.$.produce(d -> {
                    d.setLeft(input);
                    d.setNodes(Collections.singletonList(TreeNodeDraft.$.produce(n -> {
                        n.setName("nested");
                        n.setChildNodes(Collections.emptyList());
                    })));
                });
                String json = codec.writer().writeAsString(original);
                ListState restored = (ListState) codec.readerFor(Object.class).read(json);
                assertEquals(original, restored);
                assertFalse(ImmutableObjects.isLoaded(restored, "right"));
                assertThrows(UnsupportedOperationException.class, () -> restored.left().add("b"));
                assertThrows(UnsupportedOperationException.class, () -> restored.nodes().clear());
                assertThrows(UnsupportedOperationException.class, () -> restored.nodes().get(0).childNodes().clear());
                List<?> root = (List<?>) codec.readerFor(Object.class).read(codec.writer().writeAsString(original.left()));
                assertEquals(input, root);
                assertThrows(UnsupportedOperationException.class, root::clear);
            }
        }
    }

    @Test
    public void typedEntityDeserializationProtectsListsWithoutTypeIds() throws Exception {
        ListState restored = JsonCodec.jsonCodec().readerFor(ListState.class).read("{\"left\":[\"a\"]}");
        assertEquals(Collections.singletonList("a"), restored.left());
        assertThrows(UnsupportedOperationException.class, restored.left()::clear);
        assertFalse(ImmutableObjects.isLoaded(restored, "nodes"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static JsonCodec<?> polymorphicCodec() throws Exception {
        boolean v2 = JsonCodec.jsonCodec().version().name().equals("V2");
        String jackson = v2 ? "com.fasterxml.jackson" : "tools.jackson";
        Object builder = Class.forName(jackson + ".databind.json.JsonMapper").getMethod("builder").invoke(null);
        Class<?> moduleType = Class.forName(jackson + ".databind." + (v2 ? "Module" : "JacksonModule"));
        Object module = Class.forName("org.babyfish.jimmer.jackson.v" + (v2 ? "2.ImmutableModuleV2" : "3.ImmutableModuleV3"))
                .getConstructor().newInstance();
        Class<?> builderType = Class.forName(jackson + ".databind.cfg.MapperBuilder");
        builderType.getMethod("addModule", moduleType).invoke(builder, module);
        Method activate = Arrays.stream(builderType.getMethods())
                .filter(m -> m.getName().equals("activateDefaultTyping") && m.getParameterCount() == 3)
                .findFirst().orElseThrow(AssertionError::new);
        Class<?> validatorType = Class.forName(jackson + ".databind.jsontype.BasicPolymorphicTypeValidator");
        Object validatorBuilder = validatorType.getMethod("builder").invoke(null);
        validatorBuilder.getClass().getMethod("allowIfSubType", Class.class).invoke(validatorBuilder, Object.class);
        Object validator = validatorBuilder.getClass().getMethod("build").invoke(validatorBuilder);
        activate.invoke(builder, validator, Enum.valueOf((Class) activate.getParameterTypes()[1], "NON_FINAL"),
                Enum.valueOf((Class) activate.getParameterTypes()[2], "PROPERTY"));
        Object mapper = builderType.getMethod("build").invoke(builder);
        return (JsonCodec<?>) Class.forName("org.babyfish.jimmer.jackson.v" + (v2 ? "2.JsonCodecV2" : "3.JsonCodecV3"))
                .getConstructor(Class.forName(jackson + (v2 ? ".databind.ObjectMapper" : ".databind.json.JsonMapper")))
                .newInstance(mapper);
    }
}
