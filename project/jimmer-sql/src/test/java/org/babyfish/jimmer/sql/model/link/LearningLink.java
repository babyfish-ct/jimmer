package org.babyfish.jimmer.sql.model.link;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

@Entity
public interface LearningLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Student student();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Course course();

    Integer score();

    @IdView
    long studentId();

    @IdView
    long courseId();
}
