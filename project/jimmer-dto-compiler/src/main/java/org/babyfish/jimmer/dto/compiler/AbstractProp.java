package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AbstractProp {

    String getName();

    String getAlias();

    boolean isNullable();

    int getAliasLine();

    int getAliasColumn();

    List<Anno> getAnnotations();

    /** Returns the explicit DTO override, or null to inherit the model property's setting. */
    @Nullable
    default Boolean getJsonIgnore() {
        for (Anno anno : getAnnotations()) {
            if (anno.getQualifiedName().equals("org.babyfish.jimmer.json.JsonIgnore")) {
                Anno.Value value = anno.getValueMap().get("value");
                return value == null || !"false".equals(value.toString());
            }
        }
        return null;
    }

    String getDoc();

    @Nullable
    default String getFuncName() {
        return null;
    }

    @Nullable
    default DtoModifier getInputModifier() {
        return null;
    }

    @Nullable
    default BaseProp getBasePropOrNull() {
        return null;
    }
}
