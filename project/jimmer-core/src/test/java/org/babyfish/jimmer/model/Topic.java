package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Immutable;

import java.util.List;

@Immutable
public interface Topic {

    String name();

    Comment pinnedComment();

    List<Comment> comments();
}
