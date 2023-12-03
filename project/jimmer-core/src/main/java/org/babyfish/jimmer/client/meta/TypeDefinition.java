package org.babyfish.jimmer.client.meta;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface TypeDefinition {

    TypeName getTypeName();

    Kind getKind();

    boolean isApiIgnore();

    @Nullable
    List<String> getGroups();

    Map<String, Prop> getPropMap();

    List<TypeRef> getSuperTypes();

    Doc getDoc();

    Map<String, Prop> getErrorPropMap();

    Map<String, EnumConstant> getEnumConstantMap();

    enum Kind {
        IMMUTABLE,
        OBJECT,
        ENUM,
        ERROR_ENUM
    }
}
