package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.filter.dto.FileSpecification;
import org.junit.jupiter.api.Test;

public class FileTest {
    @Test
    public void fileSpecification() {
        FileSpecification fileSpecification = new FileSpecification();
        fileSpecification.setParentId(null);
    }
}
