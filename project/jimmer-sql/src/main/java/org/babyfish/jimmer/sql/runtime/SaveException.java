package org.babyfish.jimmer.sql.runtime;

public class SaveException extends ExecutionException {

    private final SavePath path;

    public SaveException(SavePath path, String message) {
        super(
                message == null || message.isEmpty() ?
                        "Save error caused by the path: \"" + path + "\"" :
                        "Save error caused by the path: \"" + path + "\": " + message
        );
        this.path = path;
    }

    public SavePath getPath() {
        return path;
    }
}
