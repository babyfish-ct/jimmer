package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.babyfish.jimmer.sql.runtime.SavePath;

public class OptimisticLockException extends SaveException {

    private final Object entityId;

    private final int entityVersion;

    private final SavePath path;

    public OptimisticLockException(SavePath path, Object entityId, int entityVersion) {
        super(
                path,
                "Cannot update the entity whose type is \"" +
                        path.getType() +
                        "\", id is \"" +
                        entityId +
                        "\" and version is \"" +
                        entityVersion +
                        "\""
        );
        this.entityId = entityId;
        this.entityVersion = entityVersion;
        this.path = path;
    }

    public ImmutableType getEntityType() {
        return path.getType();
    }

    public Object getEntityId() {
        return entityId;
    }
}
