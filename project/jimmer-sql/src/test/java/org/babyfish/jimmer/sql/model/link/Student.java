package org.babyfish.jimmer.sql.model.link;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToManyView;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@Entity
public interface Student {

    @Id
    long id();

    String name();

    @OneToMany(mappedBy = "student")
    List<LearningLink> learningLinks();

    @ManyToManyView(prop = "learningLinks")
    // @ManyToManyView(prop = "learnLinks", deeperProp = "course")
    List<Course> courses();
}
