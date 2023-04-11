package org.babyfish.jimmer.sql.model.link;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

@Entity
public interface LearningLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Nullable
    @ManyToOne(inputNotNull = true)
    @OnDissociate(DissociateAction.DELETE)
    Student student();

    @Nullable
    @ManyToOne(inputNotNull = true)
    @OnDissociate(DissociateAction.DELETE)
    Course course();

    Integer score();
}
