package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinSql;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.OnDissociate;
import org.babyfish.jimmer.sql.OneToOne;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.runtime.MutationPath;
import org.babyfish.jimmer.sql.runtime.SaveException;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

class MutationContext {

    final MutationPath path;

    MutationContext(MutationPath path) {
        this.path = path;
    }

    void throwOptimisticLockError(ImmutableSpi row) {
        throw new SaveException.OptimisticLockError(
                path,
                "Cannot update the entity whose type is \"" +
                        path.getType() +
                        "\" and id is \"" +
                        row.__get(path.getType().getIdProp().getId()) +
                        "\" because of optimistic lock error"
        );
    }

    void throwReadonlyMiddleTable() {
        throw new SaveException.ReadonlyMiddleTable(
                path,
                "The property \"" +
                        path.getProp() +
                        "\" which is based on readonly middle table cannot be saved"
        );
    }

    void throwReversedRemoteAssociation() {
        throw new SaveException.ReversedRemoteAssociation(
                path,
                "The property \"" +
                        path.getProp() +
                        "\" which is reversed(with `mappedBy`) remote(across different microservices) association " +
                        "cannot be supported by save command"
        );
    }

    void throwUnstructuredAssociation() {
        throw new SaveException.UnstructuredAssociation(
                path,
                "The property \"" +
                        path.getProp() +
                        "\" which is unstructured association(decorated by @" +
                        JoinSql.class.getName() +
                        ") " +
                        "cannot be supported by save command"
        );
    }

    void throwIllegalTargetIds(Collection<Object> illegalTargetIds) {
        if (!illegalTargetIds.isEmpty()) {
            throw new SaveException.IllegalTargetId(
                    path,
                    "Illegal ids: " + illegalTargetIds
            );
        }
    }

    void throwNoIdGenerator() {
        throw new SaveException.NoIdGenerator(
                path,
                "Cannot save \"" +
                        path.getType() + "\" " +
                        "without id because id generator is not specified"
        );
    }

    void throwNeitherIdNorKey(ImmutableType type, Set<ImmutableProp> keyProps) {
        ImmutableProp prop = path.getProp();
        if (prop != null && prop.isColumnDefinition()) {
            prop = null;
        }
        String keyNames = keyProps.stream()
                .map(ImmutableProp::getName)
                .collect(Collectors.joining(", "));
        StringBuilder builder = new StringBuilder();
        builder.append("Cannot save illegal entity object whose type is \"")
                .append(type)
                .append("\", entity with neither id nor key cannot be accepted. ")
                .append("There are ")
                .append(1 + (keyProps.isEmpty() ? 0 : 1) + (prop == null ? 0 : 1))
                .append(" way(s) to fix this problem: 1. Specify the id property \"")
                .append(type.getIdProp().getName())
                .append("\" for associated object");
        int no = 1;
        if (!keyProps.isEmpty()) {
            builder.append(", ").append(++no)
                    .append(". Specify the key properties \"")
                    .append(keyNames)
                    .append("\" for associated object");
        }
        if (prop != null) {
            builder.append(", ").append(++no).append(
                    ". Specify the associated save mode the association \"")
                    .append(prop)
                    .append("\" to \"")
                    .append(AssociatedSaveMode.APPEND.name())
                    .append("\"(function changed) or \"")
                    .append(AssociatedSaveMode.VIOLENTLY_REPLACE.name())
                    .append("\"(low performance)");
        }
        throw new SaveException.NeitherIdNorKey(
                path,
                builder.toString()
        );
    }

    void throwNoKey(ImmutableSpi spi, ImmutableProp unloadedKeyProp) {
        throw new SaveException.NoKeyProp(
                path,
                "Cannot save illegal entity object " +
                        spi +
                        " whose type is \"" +
                        spi.__type() +
                        "\", key property \"" +
                        unloadedKeyProp +
                        "\" must be loaded when id is unloaded"
        );
    }

    void throwFailedRemoteValidation() {
        throw new SaveException.FailedRemoteValidation(
                path,
                "Cannot validate the id-only associated objects of remote association \"" +
                        path.getProp() +
                        "\""
        );
    }

    void throwLongRemoteAssociation() {
        throw new SaveException.LongRemoteAssociation(
                path,
                "The property \"" +
                        path.getProp() +
                        "\" is remote(across different microservices) association, " +
                        "but it has associated object which is not id-only"
        );
    }

    void throwNullTarget() {
        throw new SaveException.NullTarget(
                path,
                "The association \"" +
                        path.getProp() +
                        "\" cannot be null, because that association is decorated by \"@" +
                        (path.getProp().getAnnotation(ManyToOne.class) != null ? ManyToOne.class : OneToOne.class).getName() +
                        "\" whose `inputNotNull` is true"
        );
    }

    void throwCannotDissociateTarget() {
        ImmutableProp mappedBy = path.getProp().getMappedBy();
        throw new SaveException.CannotDissociateTarget(
                path,
                "Cannot dissociate child objects because the dissociation action of the many-to-one property \"" +
                        mappedBy +
                        "\" is not configured as \"set null\" or \"cascade\". " +
                        "There are two ways to resolve this issue: Decorate the many-to-one property \"" +
                        mappedBy +
                        "\" by @" +
                        OnDissociate.class.getName() +
                        " whose argument is `DissociateAction.SET_NULL` or `DissociateAction.DELETE`" +
                        ", or use save command's runtime configuration to override it"
        );
    }

    void throwTargetIsNotTransferable(ImmutableSpi entity) {
        throw new SaveException.TargetIsNotTransferable(
                path,
                "Can the move the child object whose type is \"" +
                        entity.__type() +
                        "\" and id \"" +
                        entity.__get(entity.__type().getIdProp().getId()) +
                        "\" to " +
                        "another parent object because the property \"" +
                        path.getProp() +
                        "\" does not support target transfer"
        );
    }

    void throwIncompleteProperty(ImmutableProp prop, String catalog) {
        throw new SaveException.IncompleteProperty(
                path,
                "Cannot save the entity, the value of the property \"" +
                        prop +
                        "\" is illegal, the " +
                        catalog +
                        " is embeddable type but the its value is incomplete"
        );
    }

    void throwNoVersion(ImmutableProp prop) {
        throw new SaveException.IncompleteProperty(
                path,
                "Cannot save the entity, the value of the property \"" +
                        prop +
                        "\" is unloaded, the version must be specified for update/upsert"
        );
    }
}
