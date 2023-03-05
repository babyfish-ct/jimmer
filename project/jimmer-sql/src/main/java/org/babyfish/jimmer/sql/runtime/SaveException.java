package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.error.CodeBasedException;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public class SaveException extends CodeBasedException {

    private final SaveErrorCode code;

    private final ExportedSavePath exportedPath;

    private SavePath path;

    public SaveException(@NotNull SaveErrorCode code, @NotNull SavePath path, String message) {
        super(
                message == null || message.isEmpty() ?
                        "Save error caused by the path: \"" + path + "\"" :
                        "Save error caused by the path: \"" + path + "\": " + message,
                null
        );
        this.code = code;
        this.path = path;
        this.exportedPath = path.export();
    }

    public SaveException(@NotNull SaveErrorCode code, @NotNull ExportedSavePath path, String message) {
        super(message, null);
        this.code = code;
        this.exportedPath = path;
    }

    @NotNull
    @Override
    public SaveErrorCode getCode() {
        return code;
    }

    @NotNull
    public ExportedSavePath getExportedPath() {
        return exportedPath;
    }

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
}
