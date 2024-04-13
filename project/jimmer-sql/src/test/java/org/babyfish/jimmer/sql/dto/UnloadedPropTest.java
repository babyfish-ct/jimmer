package org.babyfish.jimmer.sql.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.dto.BookInput;
import org.babyfish.jimmer.sql.model.dto.DynamicBookInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnloadedPropTest {

    @Test
    public void testStaticInputWithFullValue() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9, " +
                "\"storeId\": \"de7d447f-3895-443e-bd21-16b3446413cb\", " +
                "\"authorIds\": [" +
                "    \"0773ae0b-6d8b-4a8a-8114-d5ea27cc7685\", " +
                "    \"9af72a45-07ad-447d-bf21-d2b7e62bed3e\"" +
                "]" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        BookInput input = mapper.readValue(json, BookInput.class);
        Tests.assertContentEquals(
                "BookInput(" +
                        "--->name=SQL in Action, " +
                        "--->edition=2, " +
                        "--->price=49.9, " +
                        "--->id=4470bb60-7f23-449b-840c-c511730c93b9, " +
                        "--->storeId=de7d447f-3895-443e-bd21-16b3446413cb, " +
                        "--->--->authorIds=[0773ae0b-6d8b-4a8a-8114-d5ea27cc7685, " +
                        "--->--->9af72a45-07ad-447d-bf21-d2b7e62bed3e" +
                        "--->]" +
                        ")",
                input
        );
    }

    @Test
    public void testStaticInputWithNullValue() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9, " +
                "\"storeId\": null, " +
                "\"authorIds\": [" +
                "    \"0773ae0b-6d8b-4a8a-8114-d5ea27cc7685\", " +
                "    \"9af72a45-07ad-447d-bf21-d2b7e62bed3e\"" +
                "]" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        BookInput input = mapper.readValue(json, BookInput.class);
        Tests.assertContentEquals(
                "BookInput(" +
                        "--->name=SQL in Action, " +
                        "--->edition=2, " +
                        "--->price=49.9, " +
                        "--->id=4470bb60-7f23-449b-840c-c511730c93b9, " +
                        "--->storeId=null, " +
                        "--->--->authorIds=[0773ae0b-6d8b-4a8a-8114-d5ea27cc7685, " +
                        "--->--->9af72a45-07ad-447d-bf21-d2b7e62bed3e" +
                        "--->]" +
                        ")",
                input
        );
    }

    @Test
    public void testStaticInputWithPartialValue() throws JsonProcessingException {
        String json = "{" +
                "\"id\": \"4470bb60-7f23-449b-840c-c511730c93b9\", " +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9, " +
                "\"authorIds\": [" +
                "    \"0773ae0b-6d8b-4a8a-8114-d5ea27cc7685\", " +
                "    \"9af72a45-07ad-447d-bf21-d2b7e62bed3e\"" +
                "]" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        ValueInstantiationException ex = Assertions.assertThrows(ValueInstantiationException.class, () -> {
            mapper.readValue(json, BookInput.class);
        });
        Assertions.assertEquals(
                "An object whose type is \"org.babyfish.jimmer.sql.model.dto.BookInput\" " +
                        "cannot be deserialized by Jackson. " +
                        "The current type is static input DTO so that all JSON properties " +
                        "must be specified explicitly, however, the property \"storeId\" is not " +
                        "specified by JSON explicitly. " +
                        "Please either explicitly specify the property as null in the JSON, " +
                        "or specify the current input type as dynamic in the DTO language",
                ex.getCause().getMessage()
        );
    }

    @Test
    public void testDynamicInputWithFullValue() throws JsonProcessingException {
        String json = "{" +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9, " +
                "\"storeId\": \"de7d447f-3895-443e-bd21-16b3446413cb\", " +
                "\"authorIds\": [" +
                "    \"0773ae0b-6d8b-4a8a-8114-d5ea27cc7685\", " +
                "    \"9af72a45-07ad-447d-bf21-d2b7e62bed3e\"" +
                "]" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        DynamicBookInput input = mapper.readValue(json, DynamicBookInput.class);
        Tests.assertContentEquals(
                "DynamicBookInput(" +
                        "--->name=SQL in Action, " +
                        "--->edition=2, " +
                        "--->price=49.9, " +
                        "--->storeId=de7d447f-3895-443e-bd21-16b3446413cb, " +
                        "--->--->authorIds=[0773ae0b-6d8b-4a8a-8114-d5ea27cc7685, " +
                        "--->--->9af72a45-07ad-447d-bf21-d2b7e62bed3e" +
                        "--->]" +
                        ")",
                input
        );
        Assertions.assertTrue(input.isStoreIdLoaded());
    }

    @Test
    public void testDynamicInputWithNullValue() throws JsonProcessingException {
        String json = "{" +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9, " +
                "\"storeId\": null, " +
                "\"authorIds\": [" +
                "    \"0773ae0b-6d8b-4a8a-8114-d5ea27cc7685\", " +
                "    \"9af72a45-07ad-447d-bf21-d2b7e62bed3e\"" +
                "]" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        DynamicBookInput input = mapper.readValue(json, DynamicBookInput.class);
        Tests.assertContentEquals(
                "DynamicBookInput(" +
                        "--->name=SQL in Action, " +
                        "--->edition=2, " +
                        "--->price=49.9, " +
                        "--->storeId=null, " +
                        "--->--->authorIds=[0773ae0b-6d8b-4a8a-8114-d5ea27cc7685, " +
                        "--->--->9af72a45-07ad-447d-bf21-d2b7e62bed3e" +
                        "--->]" +
                        ")",
                input
        );
        Assertions.assertTrue(input.isStoreIdLoaded());
    }

    @Test
    public void testDynamicInputWithPartialValue() throws JsonProcessingException {
        String json = "{" +
                "\"name\": \"SQL in Action\", " +
                "\"edition\": 2, " +
                "\"price\": 49.9, " +
                "\"authorIds\": [" +
                "    \"0773ae0b-6d8b-4a8a-8114-d5ea27cc7685\", " +
                "    \"9af72a45-07ad-447d-bf21-d2b7e62bed3e\"" +
                "]" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        DynamicBookInput input = mapper.readValue(json, DynamicBookInput.class);
        Tests.assertContentEquals(
                "DynamicBookInput(" +
                        "--->name=SQL in Action, " +
                        "--->edition=2, " +
                        "--->price=49.9, " +
                        "--->storeId=null, " +
                        "--->--->authorIds=[0773ae0b-6d8b-4a8a-8114-d5ea27cc7685, " +
                        "--->--->9af72a45-07ad-447d-bf21-d2b7e62bed3e" +
                        "--->]" +
                        ")",
                input
        );
        Assertions.assertFalse(input.isStoreIdLoaded());
    }
}
