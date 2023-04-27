package org.babyfish.jimmer.sql.model.type;

import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.util.List;

@MappedSuperclass
public interface Annotated {

    @Id
    long id();

    List<AnnotationNode> annotations();
}
