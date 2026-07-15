package org.babyfish.jimmer.model;

import org.babyfish.jimmer.json.JsonConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@JsonConverter(UpperCaseConverter.class)
public @interface UpperCase {
}
