package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.error.CodeBasedRuntimeException;
import org.babyfish.jimmer.internal.ClientException;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

@ClientException(
        family = "SAVE_ERROR_CODE",
        subTypes = {
                SaveException.NullTarget.class,
                SaveException.IllegalTargetId.class,
                SaveException.CannotDissociateTarget.class,
                SaveException.NoIdGenerator.class,
                SaveException.IllegalIdGenerator.class,
                SaveException.IllegalGeneratedId.class,
                SaveException.EmptyObject.class,
                SaveException.NoKeyProps.class,
                SaveException.NoNonIdProps.class,
                SaveException.NoVersion.class,
                SaveException.OptimisticLockError.class,
                SaveException.KeyNotUnique.class,
                SaveException.NeitherIdNorKey.class,
                SaveException.ReversedRemoteAssociation.class,
                SaveException.LongRemoteAssociation.class,
                SaveException.FailedRemoteValidation.class,
                SaveException.UnstructuredAssociation.class
        }
)
public abstract class SaveException extends CodeBasedRuntimeException {

    private final ExportedSavePath exportedPath;

    private SavePath path;

    public SaveException(@NotNull SavePath path, String message) {
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

    @Override
    public abstract SaveErrorCode getCode();

    @NotNull
    public ExportedSavePath getExportedPath() {
        return exportedPath;
    }

    @ApiIgnore
    @NotNull
    public SavePath getPath() {
        SavePath sp = path;
        if (sp == null) {
            path = sp = SavePath.of(exportedPath);
        }
        return sp;
    }

    @NotNull
    @Override
    public Map<String, Object> getFields() {
        return Collections.singletonMap("path", exportedPath);
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "NULL_TARGET")
    public static class NullTarget extends SaveException {

        public NullTarget(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public NullTarget(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.NULL_TARGET;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "ILLEGAL_TARGET_ID")
    public static class IllegalTargetId extends SaveException {

        public IllegalTargetId(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public IllegalTargetId(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.ILLEGAL_TARGET_ID;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "CANNOT_DISSOCIATE_TARGETS")
    public static class CannotDissociateTarget extends SaveException {

        public CannotDissociateTarget(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public CannotDissociateTarget(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.CANNOT_DISSOCIATE_TARGETS;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "NO_ID_GENERATOR")
    public static class NoIdGenerator extends SaveException {

        public NoIdGenerator(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public NoIdGenerator(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.NO_ID_GENERATOR;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "ILLEGAL_ID_GENERATOR")
    public static class IllegalIdGenerator extends SaveException {

        public IllegalIdGenerator(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public IllegalIdGenerator(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.ILLEGAL_ID_GENERATOR;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "ILLEGAL_GENERATED_ID")
    public static class IllegalGeneratedId extends SaveException {

        public IllegalGeneratedId(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public IllegalGeneratedId(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.ILLEGAL_GENERATED_ID;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "EMPTY_OBJECT")
    public static class EmptyObject extends SaveException {

        public EmptyObject(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public EmptyObject(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.EMPTY_OBJECT;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "NO_KEY_PROPS")
    public static class NoKeyProps extends SaveException {

        public NoKeyProps(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public NoKeyProps(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.NO_KEY_PROPS;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "NO_NON_ID_PROPS")
    public static class NoNonIdProps extends SaveException {

        public NoNonIdProps(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public NoNonIdProps(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.NO_NON_ID_PROPS;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "NO_VERSION")
    public static class NoVersion extends SaveException {

        public NoVersion(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public NoVersion(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.NO_VERSION;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "OPTIMISTIC_LOCK_ERROR")
    public static class OptimisticLockError extends SaveException {

        public OptimisticLockError(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public OptimisticLockError(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.OPTIMISTIC_LOCK_ERROR;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "KEY_NOT_UNIQUE")
    public static class KeyNotUnique extends SaveException {

        public KeyNotUnique(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public KeyNotUnique(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.KEY_NOT_UNIQUE;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "NEITHER_ID_NOR_KEY")
    public static class NeitherIdNorKey extends SaveException {

        public NeitherIdNorKey(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public NeitherIdNorKey(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.NEITHER_ID_NOR_KEY;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "REVERSED_REMOTE_ASSOCIATION")
    public static class ReversedRemoteAssociation extends SaveException {

        public ReversedRemoteAssociation(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public ReversedRemoteAssociation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.REVERSED_REMOTE_ASSOCIATION;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "LONG_REMOTE_ASSOCIATION")
    public static class LongRemoteAssociation extends SaveException {

        public LongRemoteAssociation(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public LongRemoteAssociation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.LONG_REMOTE_ASSOCIATION;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "FAILED_REMOTE_VALIDATION")
    public static class FailedRemoteValidation extends SaveException {

        public FailedRemoteValidation(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public FailedRemoteValidation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.FAILED_REMOTE_VALIDATION;
        }
    }

    @ClientException(family = "SAVE_ERROR_CODE", code = "UNSTRUCTURED_ASSOCIATION")
    public static class UnstructuredAssociation extends SaveException {

        public UnstructuredAssociation(@NotNull SavePath path, String message) {
            super(path, message);
        }

        public UnstructuredAssociation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getCode() {
            return SaveErrorCode.UNSTRUCTURED_ASSOCIATION;
        }
    }
}
