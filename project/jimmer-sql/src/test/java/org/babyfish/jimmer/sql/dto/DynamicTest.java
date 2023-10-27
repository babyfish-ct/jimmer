package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.dto.DynamicBookInput;
import org.babyfish.jimmer.sql.model.dto.DynamicBookInput2;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

public class DynamicTest extends Tests {

    @Test
    public void testNullByDynamicInput() {
        DynamicBookInput input = new DynamicBookInput();
        assertContentEquals(
                "{}",
                input.toEntity()
        );
    }

    @Test
    public void testNonNullByDynamicInput() {
        DynamicBookInput input = new DynamicBookInput();
        input.setName("Book");
        input.setEdition(7);
        input.setPrice(new BigDecimal("59.99"));
        input.setStoreId(UUID.fromString("641f86ab-fb30-4340-91ed-c164a8819589"));
        assertContentEquals(
                "{" +
                        "--->\"name\":\"Book\"," +
                        "--->\"edition\":7," +
                        "--->\"price\":59.99,\"" +
                        "--->store\":{" +
                        "--->--->\"id\":\"641f86ab-fb30-4340-91ed-c164a8819589\"" +
                        "--->}" +
                        "}",
                input.toEntity().toString()
        );
    }

    @Test
    public void testNullByDynamicInput2() {
        DynamicBookInput2 input = new DynamicBookInput2();
        assertContentEquals(
                "{}",
                input.toEntity()
        );
    }

    @Test
    public void testNonNullByDynamicInput2() {
        DynamicBookInput2 input = new DynamicBookInput2();
        input.setName("Book");
        input.setEdition(7);
        input.setPrice(new BigDecimal("59.99"));
        input.setParentName("Store");
        input.setParentWebsite("https://www.store.com");
        assertContentEquals(
                "{" +
                        "--->\"name\":\"Book\"," +
                        "--->\"edition\":7," +
                        "--->\"price\":59.99," +
                        "--->\"store\":{" +
                        "--->--->\"name\":\"Store\"," +
                        "--->--->\"website\":\"https://www.store.com\"" +
                        "--->}" +
                        "}",
                input.toEntity().toString()
        );
    }
}
