package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.TypeName;

public interface TypeVariable extends Type {

    String getName();

    TypeName getTypeName();
}
