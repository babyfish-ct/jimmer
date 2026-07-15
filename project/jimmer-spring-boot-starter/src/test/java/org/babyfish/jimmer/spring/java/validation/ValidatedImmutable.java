package org.babyfish.jimmer.spring.java.validation;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.NotBlank;

@Immutable
public interface ValidatedImmutable {

    @NotBlank
    String getName();
}
