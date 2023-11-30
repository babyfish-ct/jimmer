package org.babyfish.jimmer.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * By default, HTTP service classes and methods are not considered external APIs that need to be exported,
 * unless the developer manually uses @Api annotations, as follows.
 *
 * <pre>{@code
 * &#64;Api
 * &#64;RestController
 * public class TestController {
 *     &#64;Api
 *     &#64;GetMapping
 *     public String test() {
 *         return "Hello World";
 *     }
 * }
 * }</pre>
 *
 * However, you can choose to use this annotation on any of the classes, as follows:
 *
 * <pre>{@code
 * &#64;EnableImplicitApi
 * public class App {
 * }
 * }</pre>
 *
 * This eliminates the need to add @Api annotations to each HTTP service class and method.
 * Jimmer automatically treats the annotations of the web framework,
 * such as @RestController and @GetMapping, as the part of the API that needs to be exported.
 * as follows
 *
 * <pre>{@code
 * &#64;RestController
 * public class TestController {
 *     &#64;GetMapping
 *     public String test() {
 *         return "Hello World";
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface EnableImplicitApi {
}
