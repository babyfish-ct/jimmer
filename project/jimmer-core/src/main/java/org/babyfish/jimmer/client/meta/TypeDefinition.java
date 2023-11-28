package org.babyfish.jimmer.client.meta;

import java.util.List;
import java.util.Map;

public interface TypeDefinition {

    TypeName getTypeName();

    boolean isImmutable();

    boolean isApiIgnore();

    Map<String, Prop> getPropMap();

    List<TypeRef> getSuperTypes();

    Doc getDoc();
}
