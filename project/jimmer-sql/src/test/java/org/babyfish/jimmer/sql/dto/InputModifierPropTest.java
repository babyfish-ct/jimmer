package org.babyfish.jimmer.sql.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.dto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InputModifierPropTest extends Tests {

    @Test
    public void testAllProperties() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        MixedBookInput input = mapper.readValue(json, MixedBookInput.class);
        assertContentEquals(
                "MixedBookInput(" +
                        "--->id=4470bb60-7f23-449b-840c-c511730c93b9, " +
                        "--->name=SQL in Action, " +
                        "--->edition=2, " +
                        "--->price=49.9" +
                        ")",
                input
        );
        assertContentEquals(
                "{" +
                        "--->\"id\":\"4470bb60-7f23-449b-840c-c511730c93b9\"," +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":2," +
                        "--->\"price\":49.9" +
                        "}",
                input.toEntity()
        );
    }

    @Test
    public void testNullId() throws JsonProcessingException {
        String json = "{" +
                "\"id\": null, " +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        MixedBookInput input = mapper.readValue(json, MixedBookInput.class);
        assertContentEquals(
                "MixedBookInput(" +
                        "--->id=null, " +
                        "--->name=SQL in Action, " +
                        "--->edition=2, " +
                        "--->price=49.9" +
                        ")",
                input
        );
        assertContentEquals(
                "{" +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":2," +
                        "--->\"price\":49.9" +
                        "}",
                input.toEntity()
        );
    }

    @Test
    public void testMissedId() throws JsonProcessingException {
        String json = "{" +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        ValueInstantiationException ex = Assertions.assertThrows(
                ValueInstantiationException.class,
                () -> mapper.readValue(json, MixedBookInput.class)
        );
        Assertions.assertEquals(
                "An object whose type is " +
                        "\"org.babyfish.jimmer.sql.model.dto.MixedBookInput\" " +
                        "cannot be deserialized by Jackson. " +
                        "The current input has the fixed nullable property \"id\", " +
                        "it is not specified by JSON explicitly. Please either " +
                        "explicitly specify the property as null in the JSON, " +
                        "or specify the current input property as static, dynamic " +
                        "or fuzzy in the DTO language",
                ex.getCause().getMessage()
        );
    }

    @Test
    public void testNullName() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"name\": null, " +
                "\"edition\": 2, " +
                "\"price\": 49.9" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        MixedBookInput input = mapper.readValue(json, MixedBookInput.class);
        assertContentEquals(
                "MixedBookInput(" +
                        "--->id=4470bb60-7f23-449b-840c-c511730c93b9, " +
                        "--->name=null, " +
                        "--->edition=2, " +
                        "--->price=49.9" +
                        ")",
                input
        );
        assertContentEquals(
                "{" +
                        "--->\"id\":\"4470bb60-7f23-449b-840c-c511730c93b9\"," +
                        "--->\"edition\":2," +
                        "--->\"price\":49.9" +
                        "}",
                input.toEntity()
        );
    }

    @Test
    public void testMissedName() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        MixedBookInput input = mapper.readValue(json, MixedBookInput.class);
        assertContentEquals(
                "MixedBookInput(" +
                        "--->id=4470bb60-7f23-449b-840c-c511730c93b9, " +
                        "--->name=null, " +
                        "--->edition=2, " +
                        "--->price=49.9" +
                        ")",
                input
        );
        assertContentEquals(
                "{" +
                        "--->\"id\":\"4470bb60-7f23-449b-840c-c511730c93b9\"," +
                        "--->\"edition\":2," +
                        "--->\"price\":49.9" +
                        "}",
                input.toEntity()
        );
    }

    @Test
    public void testNullEdition() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": null, " +
                "\"price\": 49.9" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        MixedBookInput input = mapper.readValue(json, MixedBookInput.class);
        assertContentEquals(
                "MixedBookInput(" +
                        "--->id=4470bb60-7f23-449b-840c-c511730c93b9, " +
                        "--->name=SQL in Action, " +
                        "--->edition=null, " +
                        "--->price=49.9" +
                        ")",
                input
        );
        assertContentEquals(
                "{" +
                        "--->\"id\":\"4470bb60-7f23-449b-840c-c511730c93b9\"," +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"price\":49.9" +
                        "}",
                input.toEntity()
        );
    }

    @Test
    public void testMissedEdition() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"name\": \"SQL in Action\", " +
                "\"price\": 49.9" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        MixedBookInput input = mapper.readValue(json, MixedBookInput.class);
        assertContentEquals(
                "MixedBookInput(" +
                        "--->id=4470bb60-7f23-449b-840c-c511730c93b9, " +
                        "--->name=SQL in Action, " +
                        "--->price=49.9" +
                        ")",
                input
        );
        assertContentEquals(
                "{" +
                        "--->\"id\":\"4470bb60-7f23-449b-840c-c511730c93b9\"," +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"price\":49.9" +
                        "}",
                input.toEntity()
        );
    }

    @Test
    public void testNullPrice() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": null" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        MixedBookInput input = mapper.readValue(json, MixedBookInput.class);
        assertContentEquals(
                "MixedBookInput(" +
                        "--->id=4470bb60-7f23-449b-840c-c511730c93b9, " +
                        "--->name=SQL in Action, " +
                        "--->edition=2" +
                        ")",
                input
        );
        assertContentEquals(
                "{" +
                        "--->\"id\":\"4470bb60-7f23-449b-840c-c511730c93b9\"," +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":2" +
                        "}",
                input.toEntity()
        );
    }

    @Test
    public void testMissedPrice() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        MixedBookInput input = mapper.readValue(json, MixedBookInput.class);
        assertContentEquals(
                "MixedBookInput(" +
                        "--->id=4470bb60-7f23-449b-840c-c511730c93b9, " +
                        "--->name=SQL in Action, " +
                        "--->edition=2" +
                        ")",
                input
        );
        assertContentEquals(
                "{" +
                        "--->\"id\":\"4470bb60-7f23-449b-840c-c511730c93b9\"," +
                        "--->\"name\":\"SQL in Action\"," +
                        "--->\"edition\":2" +
                        "}",
                input.toEntity()
        );
    }

    @Test
    public void testNullableParent() throws JsonProcessingException {
        String nonNullParent = "{\"storeId\": \"4b60e499-5c24-4cd5-a922-3e49bd1d33ac\"}";
        String nullParent = "{\"storeId\": null}";
        String undefinedParent = "{}";

        assertNullableParent(
                nonNullParent,
                BookInputWithFixedParent.class,
                "BookInputWithFixedParent(storeId=4b60e499-5c24-4cd5-a922-3e49bd1d33ac)",
                "{\"store\":{\"id\":\"4b60e499-5c24-4cd5-a922-3e49bd1d33ac\"}}"
        );
        assertNullableParent(
                nullParent,
                BookInputWithFixedParent.class,
                "BookInputWithFixedParent(storeId=null)",
                "{\"store\":null}"
        );

        assertNullableParent(
                nonNullParent,
                BookInputWithStaticParent.class,
                "BookInputWithStaticParent(storeId=4b60e499-5c24-4cd5-a922-3e49bd1d33ac)",
                "{\"store\":{\"id\":\"4b60e499-5c24-4cd5-a922-3e49bd1d33ac\"}}"
        );
        assertNullableParent(
                nullParent,
                BookInputWithStaticParent.class,
                "BookInputWithStaticParent(storeId=null)",
                "{\"store\":null}"
        );
        assertNullableParent(
                undefinedParent,
                BookInputWithStaticParent.class,
                "BookInputWithStaticParent(storeId=null)",
                "{\"store\":null}"
        );

        assertNullableParent(
                nonNullParent,
                BookInputWithDynamicParent.class,
                "BookInputWithDynamicParent(storeId=4b60e499-5c24-4cd5-a922-3e49bd1d33ac)",
                "{\"store\":{\"id\":\"4b60e499-5c24-4cd5-a922-3e49bd1d33ac\"}}"
        );
        assertNullableParent(
                nullParent,
                BookInputWithDynamicParent.class,
                "BookInputWithDynamicParent(storeId=null)",
                "{\"store\":null}"
        );
        assertNullableParent(
                undefinedParent,
                BookInputWithDynamicParent.class,
                "BookInputWithDynamicParent()",
                "{}"
        );

        assertNullableParent(
                nonNullParent,
                BookInputWithFuzzyParent.class,
                "BookInputWithFuzzyParent(storeId=4b60e499-5c24-4cd5-a922-3e49bd1d33ac)",
                "{\"store\":{\"id\":\"4b60e499-5c24-4cd5-a922-3e49bd1d33ac\"}}"
        );
        assertNullableParent(
                nullParent,
                BookInputWithFuzzyParent.class,
                "BookInputWithFuzzyParent()",
                "{}"
        );
        assertNullableParent(
                undefinedParent,
                BookInputWithFuzzyParent.class,
                "BookInputWithFuzzyParent()",
                "{}"
        );
    }

    private static <T extends Input<?>> void assertNullableParent(
            String json,
            Class<T> inputType,
            String dtoJson,
            String entityJson
    ) throws JsonProcessingException {
        T input = new ObjectMapper().readValue(json, inputType);
        assertContentEquals(dtoJson, input);
        assertContentEquals(entityJson, input.toEntity());
    }
}
