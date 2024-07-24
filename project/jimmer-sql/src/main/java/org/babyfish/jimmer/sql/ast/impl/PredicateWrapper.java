package org.babyfish.jimmer.sql.ast.impl;

import org.jetbrains.annotations.NotNull;

public interface PredicateWrapper {

    Object unwrap();

    Object wrap(@NotNull Object unwrapped);
}
