package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.error.ErrorField;

@ErrorFamily
public enum SaveErrorCode {

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    NULL_TARGET,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    ILLEGAL_TARGET_ID,


    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    CANNOT_DISSOCIATE_TARGETS,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    CANNOT_CREATE_TARGET,


    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    NO_ID_GENERATOR,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    ILLEGAL_ID_GENERATOR,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    ILLEGAL_GENERATED_ID,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    EMPTY_OBJECT,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    NO_KEY_PROPS,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    NO_NON_ID_PROPS,


    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    NO_VERSION,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    ILLEGAL_VERSION,


    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    KEY_NOT_UNIQUE,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    NEITHER_ID_NOR_KEY,


    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    REVERSED_REMOTE_ASSOCIATION,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    LONG_REMOTE_ASSOCIATION,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    FAILED_REMOTE_VALIDATION,

    @ErrorField(name = "exportedPath", type = ExportedSavePath.class)
    UNSTRUCTURED_ASSOCIATION,
}
