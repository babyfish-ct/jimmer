package org.babyfish.jimmer.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.babyfish.jimmer.Immutable;

// Classic properties with prefix "get"
@Immutable
public interface Complex {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    double getReal();

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    double getImage();
}
