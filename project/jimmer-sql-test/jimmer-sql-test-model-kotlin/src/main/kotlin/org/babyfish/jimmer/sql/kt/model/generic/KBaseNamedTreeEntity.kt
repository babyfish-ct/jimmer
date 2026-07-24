package org.babyfish.jimmer.sql.kt.model.generic

import org.babyfish.jimmer.sql.MappedSuperclass

/**
 * Mapped superclass that pre-compiles a self-bounded generic tree hierarchy.
 *
 * This reproduces the shape `BaseNamedTreeNode<T> : BaseTreeNode<T>, BaseNamed where T : BaseNamedTreeNode<T>`.
 */
@MappedSuperclass
interface KBaseNamedTreeEntity<T> : KBaseId, KBaseTreeEntity<T>, KBaseNamed
    where T : KBaseNamedTreeEntity<T>
