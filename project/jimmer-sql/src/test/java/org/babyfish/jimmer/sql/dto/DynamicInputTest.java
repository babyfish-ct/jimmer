package org.babyfish.jimmer.sql.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.dto.DynamicBookInput;
import org.babyfish.jimmer.sql.model.dto.DynamicBookInput2;
import org.babyfish.jimmer.sql.model.dto.DynamicBookStoreInput;
import org.junit.jupiter.api.Test;

public class DynamicInputTest extends Tests {

    @Test
    public void testDynamicBookInput() {
        DynamicBookInput input = new DynamicBookInput();
        input.setName("SQL in Action");
        assertContentEquals(
                "{\"name\":\"SQL in Action\"}",
                input.toEntity()
        );
    }

    @Test
    public void testDynamicBookInput2() {
        DynamicBookInput2 input = new DynamicBookInput2();
        assertContentEquals(
                "{}",
                input.toEntity()
        );
    }

    @Test
    public void testDynamicBookStoreInput() {
        DynamicBookStoreInput input = new DynamicBookStoreInput();
        input.setName("MANNING");
        assertContentEquals(
                "{\"name\":\"MANNING\"}",
                input.toEntity()
        );
    }

    @Test
    public void testIssue994() throws JsonProcessingException {
        DynamicBookInput input = new DynamicBookInput();
        input.setName("MANNING");
        String json = new ObjectMapper().writeValueAsString(input);
        assertContentEquals(
                "{\"name\":\"MANNING\"}",
                json
        );
    }
}
