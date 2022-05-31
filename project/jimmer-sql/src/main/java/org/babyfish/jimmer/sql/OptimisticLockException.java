package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.ImmutableType;

public class OptimisticLockException extends RuntimeException {

    private final ImmutableType entityType;

    private final Object entityId;

    private final int entityVersion;

    private final String path;

    public OptimisticLockException(
            ImmutableType entityType,
            Object entityId,
            int entityVersion,
            String path
    ) {
        super(
                "Cannot update the entity whose type is \"" +
                        entityType +
                        "\", id is \"" +
                        entityId +
                        "\" and version is \"" +
                        entityVersion +
                        "\" at the path \"" +
                        path +
                        "\""
        );
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityVersion = entityVersion;
        this.path = path;
    }

    public ImmutableType getEntityType() {
        return entityType;
    }

    public Object getEntityId() {
        return entityId;
    }

    public String getPath() {
        return path;
    }
}
