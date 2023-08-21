package org.babyfish.jimmer.dto.compiler;

import org.antlr.v4.runtime.Token;

import java.util.List;
import java.util.Objects;

public class UserProp implements AbstractProp, AbstractPropBuilder {

    private final String alias;

    private final int line;

    private final TypeRef typeRef;

    private final List<Anno> annotations;

    public UserProp(Token alias, TypeRef typeRef, List<Anno> annotations) {
        this.alias = alias.getText();
        this.line = alias.getLine();
        this.typeRef = typeRef;
        this.annotations = annotations;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public int getAliasLine() {
        return line;
    }

    public TypeRef getTypeRef() {
        return typeRef;
    }

    public List<Anno> getAnnotations() {
        return annotations;
    }

    public UserProp build() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProp userProp = (UserProp) o;
        return line == userProp.line && Objects.equals(alias, userProp.alias) && Objects.equals(typeRef, userProp.typeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, line, typeRef);
    }

    @Override
    public String toString() {
        if (annotations.isEmpty()) {
            return alias + ": " + typeRef;
        }
        StringBuilder builder = new StringBuilder();
        for (Anno anno : annotations) {
            builder.append(anno);
        }
        builder.append(' ').append("alias").append(": ").append(typeRef);
        return builder.toString();
    }
}
