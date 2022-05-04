package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

// Classic properties with prefix "get"
@Immutable
public interface Complex {

    double getReal();

    double getImage();
}
