package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.runtime.MutationPath;
import org.babyfish.jimmer.sql.runtime.SaveException;

import java.util.Collection;
import java.util.Collections;
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

    void throwNoIdGenerator() {
        throw new SaveException.NoIdGenerator(
                path,
                "Cannot save \"" +
                        path.getType() + "\" " +
                        "without id because id generator is not specified"
        );
    }

    void throwIllegalGeneratedId(Object id) {
        throw new SaveException.IllegalGeneratedId(
                path,
                "The generated id \"" +
                        id +
                        "\" does not match the id property \"" +
                        path.getType().getIdProp() +
                        "\""
        );
    }

    void throwIllegalInterceptorBehavior(
            ImmutableProp changedIdKeyOrProp
    ) {
        throw new SaveException.IllegalIdGenerator(
                path,
                "The implementation of \"" +
                        DraftPreProcessor.class.getName() +
                        "\" or \"" +
                        DraftInterceptor.class.getName() +
                        "\" cannot modify or unload the loaded " +
                        (changedIdKeyOrProp.isId() ? "id" : "key") +
                        "property \"" +
                        changedIdKeyOrProp +
                        "\" of the draft object in the method `beforeSave`"
        );
    }

    void throwNeitherIdNorKey(ImmutableType type, Set<ImmutableProp> keyProps) {
        ImmutableProp prop = path.getProp();
        StringBuilder builder = new StringBuilder();
        builder.append("Cannot save illegal entity object whose type is \"")
                .append(type)
                .append("\", entity with neither id nor key cannot be accepted. ")
                .append("There are 3 ways to fix this problem: ")
                .append("1. Specify the id property \"")
                .append(type.getIdProp().getName())
                .append("\" for save objects");
        if (keyProps.isEmpty()) {
            builder.append("; 2. Use the annotation \"")
                    .append(Key.class.getName())
                    .append("\" to decorate some scalar or foreign key properties in entity type, ")
                    .append("or call \"setKeyProps\" of the save command, ")
                    .append("to specify the key properties of \"")
                    .append(type)
                    .append("\", and finally specified the values of key properties of saved objects");
        } else {
            String keyNames = keyProps.stream()
                    .map(ImmutableProp::getName)
                    .collect(Collectors.joining(", "));
            builder.append("; 2. Specify the value key properties \"")
                    .append(keyNames)
                    .append("\" for saved objects");
        }
        if (prop == null) {
            builder.append("; 3. Specify the aggregate-root save mode of the save command to \"")
                    .append(SaveMode.INSERT_ONLY.name())
                    .append("\"(function changed) \" or \"")
                    .append(SaveMode.NON_IDEMPOTENT_UPSERT.name())
                    .append("\"");
        } else {
            builder.append("; 3. Specify the associated save mode of the association \"")
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
                        "\", the key property \"" +
                        unloadedKeyProp +
                        "\" of object without id must be specified when " +
                        "some other key properties has been specified"
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

    void throwIllegalIdGenerator(String message) {
        throw new SaveException.IllegalIdGenerator(path, message);
    }

    void throwCannotDissociateTarget() {
        ImmutableProp backProp = path.getBackProp() != null ?
                path.getBackProp() :
                path.getProp().getMappedBy();
        throw new SaveException.CannotDissociateTarget(
                path,
                "Cannot dissociate child objects because the dissociation action of the many-to-one property \"" +
                        backProp +
                        "\" is not configured as \"set null\" or \"cascade\". " +
                        "There are two ways to resolve this issue: Decorate the many-to-one property \"" +
                        backProp +
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

    SaveException.NotUnique createConflictId(ImmutableProp idProp, Object id) {
        return new SaveException.NotUnique(
                path,
                "Cannot save the entity, the value of the id property \"" +
                        idProp +
                        "\" is \"" +
                        id +
                        "\" which already exists",
                Collections.singleton(idProp),
                id
        );
    }

    SaveException.NotUnique createConflictKey(Set<ImmutableProp> keyProps, Object key) {
        return new SaveException.NotUnique(
                path,
                "Cannot save the entity, the value of the key " +
                        (keyProps.size() == 1 ? "property" : "properties") +
                        " \"" +
                        keyProps +
                        "\" " +
                        (keyProps.size() == 1 ? "is" : "are") +
                        " \"" +
                        key +
                        "\" which already exists",
                Collections.unmodifiableSet(keyProps),
                key
        );
    }

    SaveException.IllegalTargetId createIllegalTargetId(Collection<?> targetIds) {
        return createIllegalTargetId(path, targetIds);
    }

    static SaveException.IllegalTargetId createIllegalTargetId(
            MutationPath path,
            Collection<?> targetIds
    ) {
        ImmutableProp prop = path.getProp();
        if (targetIds.size() == 1) {
            return new SaveException.IllegalTargetId(
                    path,
                    "Cannot save the entity, the associated id of the reference property \"" +
                            prop +
                            "\" is \"" +
                            targetIds.iterator().next() +
                            "\" but there is no corresponding associated object in the database",
                    prop,
                    targetIds
            );
        }
        return new SaveException.IllegalTargetId(
                path,
                "Cannot save the entity, the associated ids of the reference property \"" +
                        prop +
                        "\" are \"" +
                        targetIds +
                        "\" but there are no corresponding associated objects in the database",
                prop,
                targetIds
        );
    }
}
