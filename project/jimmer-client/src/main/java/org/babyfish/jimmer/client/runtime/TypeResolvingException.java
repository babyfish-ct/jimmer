package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.TypeName;

public class TypeResolvingException extends RuntimeException {

    private final TypeName typeName;

    private final String suffix;

    public TypeResolvingException(TypeName typeName, Throwable cause) {
        this(typeName, null, cause);
    }

    public TypeResolvingException(TypeName typeName, String suffix, Throwable cause) {
        super(
                "Cannot resolve type via the chain: " +
                        typeName + (suffix != null ? suffix : "") + (
                        cause instanceof TypeResolvingException ?
                                " -> " + ((TypeResolvingException)cause).getChain() :
                                ""
                ),
                cause
        );
        this.typeName = typeName;
        this.suffix = suffix;
    }

    public String getChain() {
        StringBuilder builder = new StringBuilder();
        builder.append(typeName.toString());
        if (suffix != null) {
            builder.append(suffix);
        }
        if (getCause() instanceof TypeResolvingException) {
            builder.append(" -> ").append(((TypeResolvingException)getCause()).getChain());
        }
        return builder.toString();
    }
}
