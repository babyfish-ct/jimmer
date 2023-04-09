package org.babyfish.jimmer.sql.model.link;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToManyView;

import java.util.List;

@Entity
public interface Course {

    @Id
    long id();

    String name();

    @ManyToManyView(LearningLink.class)
    List<Student> students();
}
