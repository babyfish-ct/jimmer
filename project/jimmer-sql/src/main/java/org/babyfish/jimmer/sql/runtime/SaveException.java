package org.babyfish.jimmer.sql.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.error.CodeBasedRuntimeException;
import org.babyfish.jimmer.ClientException;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

/**
 * The exception for save command
 */
@ClientException(
        family = "SAVE_COMMAND",
        subTypes = {
                SaveException.ReadonlyMiddleTable.class,
                SaveException.NullTarget.class,
                SaveException.IllegalTargetId.class,
                SaveException.CannotDissociateTarget.class,
                SaveException.NoIdGenerator.class,
                SaveException.IllegalIdGenerator.class,
                SaveException.IllegalGeneratedId.class,
                SaveException.EmptyObject.class,
                SaveException.NoKeyProps.class,
                SaveException.NoKeyProp.class,
                SaveException.NoNonIdProps.class,
                SaveException.NoVersion.class,
                SaveException.OptimisticLockError.class,
                SaveException.KeyNotUnique.class,
                SaveException.AlreadyExists.class,
                SaveException.NeitherIdNorKey.class,
                SaveException.ReversedRemoteAssociation.class,
                SaveException.LongRemoteAssociation.class,
                SaveException.FailedRemoteValidation.class,
                SaveException.UnstructuredAssociation.class
        }
)
public abstract class SaveException extends CodeBasedRuntimeException {

    private final ExportedSavePath exportedPath;

    private MutationPath path;

    public SaveException(@NotNull MutationPath path, String message) {
        super(
                message == null || message.isEmpty() ?
                        "Save error caused by the path: \"" + path + "\"" :
                        "Save error caused by the path: \"" + path + "\": " + message,
                null
        );
        this.path = path;
        this.exportedPath = path.export();
    }

    public SaveException(@NotNull ExportedSavePath path, String message) {
        super(message, null);
        this.exportedPath = path;
    }

    @JsonIgnore
    public abstract SaveErrorCode getSaveErrorCode();

    /**
     * The path where this exception occurs.
     */
    @NotNull
    public ExportedSavePath getExportedPath() {
        return exportedPath;
    }

    @ApiIgnore
    @NotNull
    public MutationPath getPath() {
        MutationPath sp = path;
        if (sp == null) {
            path = sp = MutationPath.of(exportedPath);
        }
        return sp;
    }

    @NotNull
    @Override
    public Map<String, Object> getFields() {
        return Collections.singletonMap("path", exportedPath);
    }

    /**
     * The association middle table is readonly
     */
    @ClientException(code = "READONLY_MIDDLE_TABLE")
    public static class ReadonlyMiddleTable extends SaveException {

        public ReadonlyMiddleTable(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public ReadonlyMiddleTable(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.READONLY_MIDDLE_TABLE;
        }
    }

    /**
     * The associated object cannot be null
     */
    @ClientException(code = "NULL_TARGET")
    public static class NullTarget extends SaveException {

        public NullTarget(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NullTarget(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NULL_TARGET;
        }
    }

    /**
     * The associated id that does not exists in database
     */
    @ClientException(code = "ILLEGAL_TARGET_ID")
    public static class IllegalTargetId extends SaveException {

        public IllegalTargetId(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public IllegalTargetId(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.ILLEGAL_TARGET_ID;
        }
    }

    /**
     * Some child objects need to be dissociated by in order to save the current object,
     * however, no dissociation behavior if configured on the many-to-one/one-to-one association
     * from child object to parent object, by either annotation or runtime overriding.
     */
    @ClientException(code = "CANNOT_DISSOCIATE_TARGETS")
    public static class CannotDissociateTarget extends SaveException {

        public CannotDissociateTarget(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public CannotDissociateTarget(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.CANNOT_DISSOCIATE_TARGETS;
        }
    }

    @ClientException(code = "NO_ID_GENERATOR")
    public static class NoIdGenerator extends SaveException {

        public NoIdGenerator(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NoIdGenerator(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NO_ID_GENERATOR;
        }
    }

    @ClientException(code = "ILLEGAL_ID_GENERATOR")
    public static class IllegalIdGenerator extends SaveException {

        public IllegalIdGenerator(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public IllegalIdGenerator(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.ILLEGAL_ID_GENERATOR;
        }
    }

    @ClientException(code = "ILLEGAL_GENERATED_ID")
    public static class IllegalGeneratedId extends SaveException {

        public IllegalGeneratedId(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public IllegalGeneratedId(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.ILLEGAL_GENERATED_ID;
        }
    }

    @ClientException(code = "EMPTY_OBJECT")
    public static class EmptyObject extends SaveException {

        public EmptyObject(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public EmptyObject(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.EMPTY_OBJECT;
        }
    }

    @ClientException(code = "NO_KEY_PROPS")
    public static class NoKeyProps extends SaveException {

        public NoKeyProps(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NoKeyProps(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NO_KEY_PROPS;
        }
    }

    @ClientException(code = "NO_KEY_PROP")
    public static class NoKeyProp extends SaveException {

        public NoKeyProp(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NoKeyProp(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NO_KEY_PROP;
        }
    }

    @ClientException(code = "NO_NON_ID_PROPS")
    public static class NoNonIdProps extends SaveException {

        public NoNonIdProps(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NoNonIdProps(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NO_NON_ID_PROPS;
        }
    }

    @ClientException(code = "NO_VERSION")
    public static class NoVersion extends SaveException {

        public NoVersion(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NoVersion(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NO_VERSION;
        }
    }

    @ClientException(code = "OPTIMISTIC_LOCK_ERROR")
    public static class OptimisticLockError extends SaveException {

        public OptimisticLockError(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public OptimisticLockError(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.OPTIMISTIC_LOCK_ERROR;
        }
    }

    @ClientException(code = "KEY_NOT_UNIQUE")
    public static class KeyNotUnique extends SaveException {

        public KeyNotUnique(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public KeyNotUnique(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.KEY_NOT_UNIQUE;
        }
    }

    /**
     * Only case when
     * 1. The transaction in trigger is enabled
     * 2. Save mode is `INSERT_ONLY` or associated mode is `APPEND`
     */
    @ClientException(code = "ALREADY_EXISTS")
    public static class AlreadyExists extends SaveException {

        public AlreadyExists(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public AlreadyExists(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.ALREADY_EXISTS;
        }
    }

    @ClientException(code = "NEITHER_ID_NOR_KEY")
    public static class NeitherIdNorKey extends SaveException {

        public NeitherIdNorKey(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NeitherIdNorKey(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NEITHER_ID_NOR_KEY;
        }
    }

    @ClientException(code = "REVERSED_REMOTE_ASSOCIATION")
    public static class ReversedRemoteAssociation extends SaveException {

        public ReversedRemoteAssociation(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public ReversedRemoteAssociation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.REVERSED_REMOTE_ASSOCIATION;
        }
    }

    @ClientException(code = "LONG_REMOTE_ASSOCIATION")
    public static class LongRemoteAssociation extends SaveException {

        public LongRemoteAssociation(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public LongRemoteAssociation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.LONG_REMOTE_ASSOCIATION;
        }
    }

    @ClientException(code = "FAILED_REMOTE_VALIDATION")
    public static class FailedRemoteValidation extends SaveException {

        public FailedRemoteValidation(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public FailedRemoteValidation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.FAILED_REMOTE_VALIDATION;
        }
    }

    @ClientException(code = "UNSTRUCTURED_ASSOCIATION")
    public static class UnstructuredAssociation extends SaveException {

        public UnstructuredAssociation(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public UnstructuredAssociation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.UNSTRUCTURED_ASSOCIATION;
        }
    }
}
