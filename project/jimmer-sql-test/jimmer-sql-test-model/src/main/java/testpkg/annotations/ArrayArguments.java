package testpkg.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ArrayArguments {

    int[] value() default {7};

    Class<? extends Number>[] numberClass() default {};

    N[] n() default {};

    M m() default @M;

    @Retention(RetentionPolicy.RUNTIME)
    @interface N {
        int[] value() default {9};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface M {
        int value() default 7;
    }
}
