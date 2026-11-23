package org.babyfish.jimmer;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.model.BaseTypeDraft;
import org.babyfish.jimmer.model.DerivedType;
import org.babyfish.jimmer.model.DerivedTypeDraft;
import org.babyfish.jimmer.sql.meta.impl.MetadataLiterals;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class MetaTest {

    @Test
    public void jsonDefaultValuesAreIsolatedBetweenContextClassLoaders() {
        MetadataLiterals.JsonLiteral literal = (MetadataLiterals.JsonLiteral)
                MetadataLiterals.parse(List.class, false, "[\"item\"]");
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        ClassLoader parent = MetaTest.class.getClassLoader();
        ClassLoader firstContext = new ClassLoader(parent) {};
        ClassLoader secondContext = new ClassLoader(parent) {};
        try {
            thread.setContextClassLoader(firstContext);
            List<?> first = (List<?>) literal.defaultValue().getValue();
            first.clear();
            thread.setContextClassLoader(secondContext);
            Assertions.assertEquals(Collections.singletonList("item"), literal.defaultValue().getValue());
        } finally {
            thread.setContextClassLoader(original);
        }
    }

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
