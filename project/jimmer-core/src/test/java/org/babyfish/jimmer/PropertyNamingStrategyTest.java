package org.babyfish.jimmer;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.model.AssociationInput;
import org.babyfish.jimmer.model.AssociationInputDraft;
import org.babyfish.jimmer.jackson.codec.PropertyNamingCustomization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;
import static org.babyfish.jimmer.jackson.codec.PropertyNamingCustomization.PropertyNaming;

public class PropertyNamingStrategyTest {

    private static final AssociationInput INPUT = AssociationInputDraft.$.produce(input -> {
        input.setParentId(1L).setChildIds(Arrays.asList(2L, 3L));
    });

    @Test
    public void testLowerCamel() throws Exception {
        JsonCodec<?> codec = jsonCodec()
                .withCustomizations(new PropertyNamingCustomization(PropertyNaming.LOWER_CAMEL_CASE));
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

    @Test
    public void testUpperCamel() throws Exception {
        JsonCodec<?> codec = jsonCodec()
                .withCustomizations(new PropertyNamingCustomization(PropertyNaming.UPPER_CAMEL_CASE));
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

    @Test
    public void testLowerCase() throws Exception {
        JsonCodec<?> codec = jsonCodec()
                .withCustomizations(new PropertyNamingCustomization(PropertyNaming.LOWER_CASE));
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

    @Test
    public void testSnakeCase() throws Exception {
        JsonCodec<?> codec = jsonCodec()
                .withCustomizations(new PropertyNamingCustomization(PropertyNaming.SNAKE_CASE));
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

    @Test
    public void testExplicitCodecForImmutableObjectsToString() {
        JsonCodec<?> codec = jsonCodec()
                .withCustomizations(new PropertyNamingCustomization(PropertyNaming.SNAKE_CASE));
        Assertions.assertEquals(
                "{\"parent_id\":1,\"child_ids\":[2,3]}",
                ImmutableObjects.toString(INPUT, codec)
        );
    }

    @Test
    public void testKebabCase() throws Exception {
        JsonCodec<?> codec = jsonCodec()
                .withCustomizations(new PropertyNamingCustomization(PropertyNaming.KEBAB_CASE));
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

    @Test
    public void testLowerDot() throws Exception {
        JsonCodec<?> codec = jsonCodec()
                .withCustomizations(new PropertyNamingCustomization(PropertyNaming.LOWER_DOT_CASE));
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
