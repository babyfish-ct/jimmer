import type {ExportedSavePath} from './model/static/';

export type AllErrors = {
        family: 'SAVE_COMMAND', 
        code: 'NULL_TARGET', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'ILLEGAL_TARGET_ID', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'CANNOT_DISSOCIATE_TARGETS', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'NO_ID_GENERATOR', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'ILLEGAL_ID_GENERATOR', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'ILLEGAL_GENERATED_ID', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'EMPTY_OBJECT', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'NO_KEY_PROPS', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'NO_NON_ID_PROPS', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'NO_VERSION', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'OPTIMISTIC_LOCK_ERROR', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'KEY_NOT_UNIQUE', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'NEITHER_ID_NOR_KEY', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'REVERSED_REMOTE_ASSOCIATION', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'LONG_REMOTE_ASSOCIATION', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'FAILED_REMOTE_VALIDATION', 
        exportedPath: ExportedSavePath
    } | {
        family: 'SAVE_COMMAND', 
        code: 'UNSTRUCTURED_ASSOCIATION', 
        exportedPath: ExportedSavePath
    };
export type ApiErrors = {
    'authorService': {
        'saveAuthor': AllErrors & ({
                family: 'SAVE_COMMAND', 
                code: 'NULL_TARGET', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_TARGET_ID', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'CANNOT_DISSOCIATE_TARGETS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_ID_GENERATOR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_ID_GENERATOR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_GENERATED_ID', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'EMPTY_OBJECT', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_KEY_PROPS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_NON_ID_PROPS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_VERSION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'OPTIMISTIC_LOCK_ERROR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'KEY_NOT_UNIQUE', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NEITHER_ID_NOR_KEY', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'REVERSED_REMOTE_ASSOCIATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'LONG_REMOTE_ASSOCIATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'FAILED_REMOTE_VALIDATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'UNSTRUCTURED_ASSOCIATION', 
                readonly [key:string]: any
            })
    }, 
    'bookService': {
        'saveBook': AllErrors & ({
                family: 'SAVE_COMMAND', 
                code: 'NULL_TARGET', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_TARGET_ID', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'CANNOT_DISSOCIATE_TARGETS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_ID_GENERATOR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_ID_GENERATOR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_GENERATED_ID', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'EMPTY_OBJECT', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_KEY_PROPS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_NON_ID_PROPS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_VERSION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'OPTIMISTIC_LOCK_ERROR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'KEY_NOT_UNIQUE', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NEITHER_ID_NOR_KEY', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'REVERSED_REMOTE_ASSOCIATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'LONG_REMOTE_ASSOCIATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'FAILED_REMOTE_VALIDATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'UNSTRUCTURED_ASSOCIATION', 
                readonly [key:string]: any
            }), 
        'saveCompositeBook': AllErrors & ({
                family: 'SAVE_COMMAND', 
                code: 'NULL_TARGET', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_TARGET_ID', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'CANNOT_DISSOCIATE_TARGETS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_ID_GENERATOR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_ID_GENERATOR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_GENERATED_ID', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'EMPTY_OBJECT', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_KEY_PROPS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_NON_ID_PROPS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_VERSION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'OPTIMISTIC_LOCK_ERROR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'KEY_NOT_UNIQUE', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NEITHER_ID_NOR_KEY', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'REVERSED_REMOTE_ASSOCIATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'LONG_REMOTE_ASSOCIATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'FAILED_REMOTE_VALIDATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'UNSTRUCTURED_ASSOCIATION', 
                readonly [key:string]: any
            })
    }, 
    'bookStoreService': {
    }, 
    'treeService': {
        'saveTree': AllErrors & ({
                family: 'SAVE_COMMAND', 
                code: 'NULL_TARGET', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_TARGET_ID', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'CANNOT_DISSOCIATE_TARGETS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_ID_GENERATOR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_ID_GENERATOR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'ILLEGAL_GENERATED_ID', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'EMPTY_OBJECT', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_KEY_PROPS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_NON_ID_PROPS', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NO_VERSION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'OPTIMISTIC_LOCK_ERROR', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'KEY_NOT_UNIQUE', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'NEITHER_ID_NOR_KEY', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'REVERSED_REMOTE_ASSOCIATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'LONG_REMOTE_ASSOCIATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'FAILED_REMOTE_VALIDATION', 
                readonly [key:string]: any
            } | {
                family: 'SAVE_COMMAND', 
                code: 'UNSTRUCTURED_ASSOCIATION', 
                readonly [key:string]: any
            })
    }
};
