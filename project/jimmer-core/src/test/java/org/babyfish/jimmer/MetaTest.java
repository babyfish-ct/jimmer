package org.babyfish.jimmer;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.model.BaseTypeDraft;
import org.babyfish.jimmer.model.DerivedType;
import org.babyfish.jimmer.model.DerivedTypeDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetaTest {

    @Test
    public void test() {
        Assertions.assertSame(
                ImmutableType.get(DerivedType.class),
                ImmutableType.get(Type1.class)
        );
        Assertions.assertSame(
                ImmutableType.get(DerivedType.class),
                ImmutableType.get(Type2.class)
        );
    }

    private static class Type1 implements DerivedTypeDraft, BaseTypeDraft {}

    private static class Type2 implements BaseTypeDraft, DerivedTypeDraft {}
}
