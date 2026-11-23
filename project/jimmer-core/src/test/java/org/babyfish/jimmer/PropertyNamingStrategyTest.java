package org.babyfish.jimmer;

import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.JsonType;
import org.babyfish.jimmer.model.AssociationInput;
import org.babyfish.jimmer.model.AssociationInputDraft;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import java.util.Arrays;
import java.util.Collections;

import static org.babyfish.jimmer.support.JacksonBackend.Naming.*;

public class PropertyNamingStrategyTest {

    private static final AssociationInput INPUT = AssociationInputDraft.$.produce(input -> {
        input.setParentId(1L).setChildIds(Arrays.asList(2L, 3L));
    });

    @JacksonTest
    public void testLowerCamel(JacksonBackend backend) throws Exception {
        JsonCodec codec = backend.naming(LOWER_CAMEL_CASE);
        String json = codec.writer().writeAsString(INPUT);
        Assertions.assertEquals(
                "{\"parentId\":1,\"childIds\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                codec.readerFor(AssociationInput.class).read(json)
        );
    }

    @JacksonTest
    public void testUpperCamel(JacksonBackend backend) throws Exception {
        JsonCodec codec = backend.naming(UPPER_CAMEL_CASE);
        String json = codec.writer().writeAsString(INPUT);
        Assertions.assertEquals(
                "{\"ParentId\":1,\"ChildIds\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                codec.readerFor(AssociationInput.class).read(json)
        );
    }

    @JacksonTest
    public void testLowerCase(JacksonBackend backend) throws Exception {
        JsonCodec codec = backend.naming(LOWER_CASE);
        String json = codec.writer().writeAsString(INPUT);
        Assertions.assertEquals(
                "{\"parentid\":1,\"childids\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                codec.readerFor(AssociationInput.class).read(json)
        );
    }

    @JacksonTest
    public void testSnakeCase(JacksonBackend backend) throws Exception {
        JsonCodec codec = backend.naming(SNAKE_CASE);
        String json = codec.writer().writeAsString(INPUT);
        Assertions.assertEquals(
                "{\"parent_id\":1,\"child_ids\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                codec.readerFor(AssociationInput.class).read(json)
        );
    }

    @JacksonTest
    public void testDeclaredInterfaceAndList(JacksonBackend backend) throws Exception {
        JsonCodec codec = backend.naming(SNAKE_CASE);
        JsonType<AssociationInput> type = JsonType.of(AssociationInput.class);
        String expected = "{\"parent_id\":1,\"child_ids\":[2,3]}";
        Assertions.assertEquals(expected, codec.writer(type).writeAsString(INPUT));
        Assertions.assertEquals("[" + expected + "]",
                codec.writer(JsonType.listOf(type)).writeAsString(Collections.singletonList(INPUT)));
        AssociationInput partial = AssociationInputDraft.$.produce(draft -> draft.setParentId(1L));
        Assertions.assertEquals("{\"parent_id\":1}", codec.writer(type).writeAsString(partial));
        AssociationInput hidden = AssociationInputDraft.$.produce(INPUT, draft -> DraftObjects.hide(draft, "parentId"));
        Assertions.assertEquals("{\"child_ids\":[2,3]}", codec.writer(type).writeAsString(hidden));
    }

    @JacksonTest
    public void testExplicitCodecForImmutableObjectsToString(JacksonBackend backend) {
        JsonCodec codec = backend.naming(SNAKE_CASE);
        Assertions.assertEquals(
                "{\"parent_id\":1,\"child_ids\":[2,3]}",
                ImmutableObjects.toString(INPUT, codec)
        );
    }

    @JacksonTest
    public void testKebabCase(JacksonBackend backend) throws Exception {
        JsonCodec codec = backend.naming(KEBAB_CASE);
        String json = codec.writer().writeAsString(INPUT);
        Assertions.assertEquals(
                "{\"parent-id\":1,\"child-ids\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                codec.readerFor(AssociationInput.class).read(json)
        );
    }

    @JacksonTest
    public void testLowerDot(JacksonBackend backend) throws Exception {
        JsonCodec codec = backend.naming(LOWER_DOT_CASE);
        String json = codec.writer().writeAsString(INPUT);
        Assertions.assertEquals(
                "{\"parent.id\":1,\"child.ids\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                codec.readerFor(AssociationInput.class).read(json)
        );
    }

}
