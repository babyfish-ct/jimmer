package org.babyfish.jimmer;

import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.model.ListState;
import org.babyfish.jimmer.model.ListStateDraft;
import org.babyfish.jimmer.model.TreeNodeDraft;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PolymorphicListTest {

    @JacksonTest
    public void defaultTypingRestoresRootAndNestedLists(JacksonBackend backend) throws Exception {
        JsonCodec codec = backend.polymorphicCodec();
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

    @JacksonTest
    public void typedEntityDeserializationProtectsListsWithoutTypeIds(JacksonBackend backend) throws Exception {
        ListState restored = backend.codec.readerFor(ListState.class).read("{\"left\":[\"a\"]}");
        assertEquals(Collections.singletonList("a"), restored.left());
        assertThrows(UnsupportedOperationException.class, restored.left()::clear);
        assertFalse(ImmutableObjects.isLoaded(restored, "nodes"));
    }
}
