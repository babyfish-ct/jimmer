package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@Immutable
public interface AssociationInput {

    @Min(0)
    @Max(100)
    long parentId();

    List<Long> childIds();
}
