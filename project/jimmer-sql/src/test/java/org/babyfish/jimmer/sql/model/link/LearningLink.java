package org.babyfish.jimmer.sql.model.link;

import org.babyfish.jimmer.sql.*;

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

    int score();
}
