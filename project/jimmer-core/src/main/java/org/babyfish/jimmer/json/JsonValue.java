package org.babyfish.jimmer.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Uses the result of a public, parameterless enum method as its JSON value.
 * Each constant must have a distinct, non-null value. Supported return types are
 * String, byte, short, int, long, and their boxed equivalents.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JsonValue {}
