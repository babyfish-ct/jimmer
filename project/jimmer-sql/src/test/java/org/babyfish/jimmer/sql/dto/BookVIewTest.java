package org.babyfish.jimmer.sql.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.dto.BookViewForIssue843;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class BookVIewTest extends Tests {

    @Test
    public void testForIssue843() throws JsonProcessingException {
        BookViewForIssue843 view = new BookViewForIssue843();
        view.setId(Constants.programmingTypeScriptId2);
        view.setName("Programming TypeScript");
        view.setEdition(2);
        view.setPrice(new BigDecimal("59.99"));
        assertContentEquals(
                "{" +
                        "--->\"id\":\"058ecfd0-047b-4979-a7dc-46ee24d08f08\"," +
                        "--->\"name\":\"Programming TypeScript\"," +
                        "--->\"price\":59.99" +
                        "}",
                new ObjectMapper().writeValueAsString(view)
        );
    }
}
