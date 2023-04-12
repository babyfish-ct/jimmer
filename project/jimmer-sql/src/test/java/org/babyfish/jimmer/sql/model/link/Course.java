package org.babyfish.jimmer.sql.model.link;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToManyView;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@Entity
public interface Course {

    @Id
    long id();

    String name();

    int academicCredit();

    @OneToMany(mappedBy = "course")
    List<LearningLink> learningLinks();

    @ManyToManyView(prop = "learningLinks", deeperProp = "student")
    // or @ManyToManyView(prop = "learningLinks")
    List<Student> students();
}
