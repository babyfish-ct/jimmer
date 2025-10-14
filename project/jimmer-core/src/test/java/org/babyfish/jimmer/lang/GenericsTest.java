package org.babyfish.jimmer.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class GenericsTest {

    @Test
    public void testStringList() {
        Type[] arguments = Generics.getTypeArguments(StringList.class, List.class);
        Assertions.assertArrayEquals(new Type[] { String.class }, arguments);
    }

    @Test
    public void testStringListList() {
        Type[] arguments = Generics.getTypeArguments(StringListList.class, List.class);
        Assertions.assertEquals(1, arguments.length);
        Assertions.assertInstanceOf(ParameterizedType.class, arguments[0]);
        arguments = Generics.getTypeArguments(arguments[0], List.class);
        Assertions.assertArrayEquals(new Type[] { String.class }, arguments);
    }

    @Test
    public void testDoubleMatrix() {
        Type[] arguments = Generics.getTypeArguments(DoubleMatrix.class, Data.class);
        Assertions.assertEquals(1, arguments.length);
        Type argument = arguments[0];
        Assertions.assertEquals("java.lang.Double[][]", argument.getTypeName());
    }

    @Test
    public void testEnumType() {
        Type[] arguments = Generics.getTypeArguments(EnumType.class, Enum.class);
        Assertions.assertEquals(1, arguments.length);
        Assertions.assertEquals(EnumType.class, arguments[0]);
    }

    private static abstract class StringList implements List<String> {}

    private static abstract class StringListList implements List<List<String>> {}

    private interface Data<T> { T get(); }

    private interface Matrix<T> extends Data<T[][]> {}

    private interface DoubleMatrix extends Matrix<Double> {}

    private enum EnumType {}
}
