package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.dto.BundledBookView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DtoBundleTest {

    @Test
    public void testBundledDtoIsGenerated() {
        Assertions.assertEquals("BundledBookView", BundledBookView.class.getSimpleName());
    }
}
