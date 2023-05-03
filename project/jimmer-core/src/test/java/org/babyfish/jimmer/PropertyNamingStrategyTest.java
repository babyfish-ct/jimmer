package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.model.AssociationInput;
import org.babyfish.jimmer.model.AssociationInputDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class PropertyNamingStrategyTest {

    private static final AssociationInput INPUT = AssociationInputDraft.$.produce(input -> {
        input.setParentId(1L).setChildIds(Arrays.asList(2L, 3L));
    });

    @Test
    public void testLowerCamel() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ImmutableModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        String json = mapper.writeValueAsString(INPUT);
        Assertions.assertEquals(
                "{\"parentId\":1,\"childIds\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                mapper.readValue(json, AssociationInput.class)
        );
    }

    @Test
    public void testUpperCamel() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ImmutableModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
        String json = mapper.writeValueAsString(INPUT);
        Assertions.assertEquals(
                "{\"ParentId\":1,\"ChildIds\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                mapper.readValue(json, AssociationInput.class)
        );
    }

    @Test
    public void testLowerCase() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ImmutableModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CASE);
        String json = mapper.writeValueAsString(INPUT);
        Assertions.assertEquals(
                "{\"parentid\":1,\"childids\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                mapper.readValue(json, AssociationInput.class)
        );
    }

    @Test
    public void testSnakeCase() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ImmutableModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        String json = mapper.writeValueAsString(INPUT);
        Assertions.assertEquals(
                "{\"parent_id\":1,\"child_ids\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                mapper.readValue(json, AssociationInput.class)
        );
    }

    @Test
    public void testKebabCase() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ImmutableModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        String json = mapper.writeValueAsString(INPUT);
        Assertions.assertEquals(
                "{\"parent-id\":1,\"child-ids\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                mapper.readValue(json, AssociationInput.class)
        );
    }

    @Test
    public void testLowerDot() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ImmutableModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_DOT_CASE);
        String json = mapper.writeValueAsString(INPUT);
        Assertions.assertEquals(
                "{\"parent.id\":1,\"child.ids\":[2,3]}",
                json
        );
        Assertions.assertEquals(
                INPUT,
                mapper.readValue(json, AssociationInput.class)
        );
    }
}
