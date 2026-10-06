package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.dto.UserView;
import org.babyfish.jimmer.sql.model.dto.ArrayArgumentsArrayView;
import org.babyfish.jimmer.sql.model.dto.ArrayArgumentsDefaultView;
import org.babyfish.jimmer.sql.model.dto.ArrayArgumentsEmptyView;
import org.babyfish.jimmer.sql.model.dto.ArrayArgumentsPositionalView;
import org.babyfish.jimmer.sql.model.dto.ArrayArgumentsSingletonView;
import org.babyfish.jimmer.sql.model.filter.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testpkg.annotations.Serializable;
import testpkg.annotations.ArrayArguments;

public class AnnotationTest {

    @Test
    public void testArrayArguments() {
        ArrayArguments singleton = ArrayArgumentsSingletonView.class.getAnnotation(ArrayArguments.class);
        Assertions.assertArrayEquals(new int[]{10}, singleton.value());
        Assertions.assertArrayEquals(new Class<?>[]{int.class}, singleton.numberClass());
        Assertions.assertArrayEquals(new int[]{20}, singleton.n()[0].value());
        Assertions.assertEquals(7, singleton.m().value());

        ArrayArguments positional = ArrayArgumentsPositionalView.class.getAnnotation(ArrayArguments.class);
        Assertions.assertArrayEquals(new int[]{10, 20}, positional.value());
        Assertions.assertArrayEquals(new int[]{30, 40}, positional.n()[0].value());

        ArrayArguments array = ArrayArgumentsArrayView.class.getAnnotation(ArrayArguments.class);
        Assertions.assertArrayEquals(new int[]{1, 2}, array.value());
        Assertions.assertArrayEquals(new Class<?>[]{int.class, long.class}, array.numberClass());
        Assertions.assertEquals(2, array.n().length);
        Assertions.assertArrayEquals(new int[]{3, 4}, array.n()[0].value());
        Assertions.assertArrayEquals(new int[]{5, 6}, array.n()[1].value());

        ArrayArguments empty = ArrayArgumentsEmptyView.class.getAnnotation(ArrayArguments.class);
        Assertions.assertArrayEquals(new int[0], empty.value());
        Assertions.assertEquals(0, empty.numberClass().length);
        Assertions.assertEquals(0, empty.n().length);

        ArrayArguments defaults = ArrayArgumentsDefaultView.class.getAnnotation(ArrayArguments.class);
        Assertions.assertArrayEquals(new int[]{7}, defaults.value());
        Assertions.assertArrayEquals(new int[]{9}, defaults.n()[0].value());
    }

    @Test
    public void testEntityAnnotations() throws NoSuchMethodException {
        Serializable a1 = UserView.class.getAnnotation(Serializable.class);
        Serializable a2 = UserView.class.getMethod("getName").getAnnotation(Serializable.class);
        Assertions.assertEquals(User.class, a1.with());
        Assertions.assertEquals(String.class, a2.with());
    }
}
