package org.babyfish.jimmer.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface ExportDoc {

    boolean excluded() default false;
}
