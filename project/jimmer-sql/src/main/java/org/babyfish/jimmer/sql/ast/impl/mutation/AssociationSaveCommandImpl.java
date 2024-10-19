package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.ast.mutation.AssociationSaveCommand;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;

class AssociationSaveCommandImpl implements AssociationSaveCommand {

    private AssociationExecutable executable;

    public AssociationSaveCommandImpl(AssociationExecutable executable) {
        this.executable = executable;
    }

    @Override
    public Integer execute(Connection con) {
        return executable
                .sqlClient
                .getConnectionManager()
                .execute(con, this::executeImpl);
    }

    @Override
    public AssociationSaveCommand ignoreConflict(@Nullable Boolean checkExistence) {
        AssociationExecutable newExecutable = executable.setCheckExistence(checkExistence);
        if (newExecutable == executable) {
            return this;
        }
        return new AssociationSaveCommandImpl(newExecutable);
    }

    @Override
    public AssociationSaveCommand deleteUnnecessary(@Nullable Boolean deleteUnnecessary) {
        AssociationExecutable newExecutable = executable.setDeleteUnnecessary(deleteUnnecessary);
        if (newExecutable == executable) {
            return this;
        }
        return new AssociationSaveCommandImpl(newExecutable);
    }

    private Integer executeImpl(Connection con) {
        return executable.execute(con);
    }
}
