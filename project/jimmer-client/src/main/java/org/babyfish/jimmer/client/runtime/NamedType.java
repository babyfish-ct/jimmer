package org.babyfish.jimmer.client.runtime;

import java.util.List;

public interface NamedType extends Type {

    List<String> getSimpleNames();
}
