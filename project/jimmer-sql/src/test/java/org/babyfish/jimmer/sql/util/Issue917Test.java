package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.model.Immutables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Issue917Test {

    @Test
    public void test() {
        Immutables.createTreeNode(draft -> {
            draft.setParentId(2L);
            Assertions.assertEquals(
                    "{\"parent\":{\"id\":2}}",
                    draft.toString()
            );
        });
    }
}
