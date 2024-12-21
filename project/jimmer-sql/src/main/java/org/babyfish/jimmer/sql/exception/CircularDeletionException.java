package org.babyfish.jimmer.sql.exception;

import org.babyfish.jimmer.sql.runtime.MutationPath;
import org.jetbrains.annotations.NotNull;

public class CircularDeletionException extends RuntimeException {

    private final MutationPath path;

    private final Object id;

    public CircularDeletionException(MutationPath path, Object id) {
        super(
                "Circular deletion is found, repeated object \"" +
                        path.getType() +
                        "(" +
                        id +
                        ")\" at the path \"" +
                        path +
                        "\""
        );
        this.path = path;
        this.id = id;
    }

    @NotNull
    public MutationPath getPath() {
        return path;
    }

    @NotNull
    public Object getId() {
        return id;
    }
}
