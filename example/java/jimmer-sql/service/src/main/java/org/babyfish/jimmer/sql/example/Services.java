package org.babyfish.jimmer.sql.example;

import org.babyfish.jimmer.sql.EnableDtoGeneration;

/**
 * The entity interfaces are defined in another subproject `model`,
 * the current subproject has no types decorated by `@Entity`, `@MappedSuperclass`,
 * `@Embeddable`, `@Immutable`, or `@ErrorFamily` so that `jimmer-apt`
 * cannot handle current subproject.
 *
 * <p>This empty classes uses `@EnableDtoGeneration` to enable the `jimmer-apt` manually.</p>
 *
 * <p>This is only required by java, not kotlin(kotlin uses KSP, not APT). </p>
 */
@EnableDtoGeneration
public interface Services {
}
