package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.Size;

@Immutable
public interface Image {

    @Size(min = 32, max = 32)
    String hash();
}
