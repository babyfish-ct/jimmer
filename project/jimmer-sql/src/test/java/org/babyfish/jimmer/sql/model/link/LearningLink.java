package org.babyfish.jimmer.sql.model.link;

import org.babyfish.jimmer.sql.*;

@Entity
public interface LearningLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @ManyToOne
    Student student();

    @ManyToOne
    Course course();

    int score();
}
