package org.babyfish.jimmer.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Jimmer can automatically generate OpenAPI documentation
 * and TypeScript code for REST services, utilizing Java or
 * Kotlin code documentation comments in this process.</p>
 *
 * <p>For types directly located within a web project, this
 * is a natural process because documentation comments are
 * visible to the compiler.</p>
 *
 * <p>However, real projects are separated into modules,
 * for non-web modules, once compilation is complete,
 * documentation comments are lost. Web module cannot access
 * documentation comments from non-web modules through jar
 * dependencies.</p>
 *
 * <p>For this purpose, you can use this annotation to decorate
 * packages or types, writing documentation comments of some types
 * to the {@code META-INF/jimmer/doc.properties} file in the
 * target jar file.</p>
 *
 * <p><b>Note</b> that only documentation comments for</p>
 * <ul>
 *   <li>Types</li>
 *   <li>Java non-static fields</li>
 *   <li>Java getters</li>
 *   <li>Kotlin properties</li>
 * </ul>
 * will be written.
 *
 * <p>This annotation has an {@link #excluded()} parameter:</p>
 * <ul>
 *     <li>If false <i>(which is the default value)</i>,
 *     it indicates a positive configuration,
 *     allowing storage of documentation comments for certain types.</li>
 *     <li>If true, it indicates a negative configuration,
 *     prohibiting storage of documentation comments for certain types.</li>
 * </ul>
 *
 * <p>This annotation can decorate both types and packages:</p>
 * <ul>
 *     <li><b>When decorating types</b>:
 *          Class-level configuration affects not only the current class but also all inner classes.
 *     </li>
 *     <li><b>When decorating packages</b>:
 *          <ul>
 *              <li>
 *                  Java: Create a {@code package-info.java} file in the package
 *                  and add the following code
 *                  <pre>{@code
 *                  @ExportDoc
 *                  package yourPackageName;
 *
 *                  import org.babyfish.jimmer.client.ExportDoc;
 *                  }</pre>
 *              </li>
 *              <li>
 *                  Kotlin: Create a file in the package<i>(recommended name is {@code package.kt}</i>,
 *                  and add the following code
 *                  <pre>{@code
 *                  @file: ExportDoc // `file:` is important
 *                  package yourPackageName
 *
 *                  import org.babyfish.jimmer.client.ExportDoc
 *                  }</pre>
 *              </li>
 *          </ul>
 *          Note: Package-level configuration affects not only the current package but also deeper packages.
 *     </li>
 * </ul>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface ExportDoc {

    /**
     * <ul>
     *     <li>false: Positive configuration, allowed</li>
     *     <li>true: Negative configuration, not allowed</li>
     * </ul>
     */
    boolean excluded() default false;
}
