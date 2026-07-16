package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.client.java.model.dto.filter.AuthorFromFilteredFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DtoTargetFilterTest {

    @Test
    public void testAllowedDeclarationFromExcludedDefaultTargetFile() {
        Assertions.assertEquals(
                "org.babyfish.jimmer.client.java.model.dto.filter.AuthorFromFilteredFile",
                AuthorFromFilteredFile.class.getName()
        );
    }
}
