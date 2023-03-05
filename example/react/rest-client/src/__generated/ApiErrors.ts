import type { ExportedSavePath } from './model/static';

export type AllErrors = 
    {
        readonly family: "BusinessErrorCode",
        readonly code: "GLOBAL_TENANT_REQUIRED"
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "CANNOT_CREATE_TARGET",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "CANNOT_DISSOCIATE_TARGETS",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "ILLEGAL_GENERATED_ID",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "ILLEGAL_ID_GENERATOR",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "ILLEGAL_TARGET_ID",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "ILLEGAL_VERSION",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "KEY_NOT_UNIQUE",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "NEITHER_ID_NOR_KEY",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "NO_ID_GENERATOR",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "NO_KEY_PROPS",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "NO_NON_ID_PROPS",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "NO_VERSION",
        readonly "exportedPath": ExportedSavePath
    } | 
    {
        readonly family: "SaveErrorCode",
        readonly code: "NULL_TARGET",
        readonly "exportedPath": ExportedSavePath
    }
;

export type ApiErrors = {
    "authorService": {
        "saveAuthor": AllErrors & (
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NULL_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_TARGET_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_DISSOCIATE_TARGETS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_CREATE_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_GENERATED_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_KEY_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_NON_ID_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'KEY_NOT_UNIQUE',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NEITHER_ID_NOR_KEY',
                readonly [key:string]: any
            }
        )
    },
    "bookService": {
        "saveBook": AllErrors & (
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NULL_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_TARGET_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_DISSOCIATE_TARGETS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_CREATE_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_GENERATED_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_KEY_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_NON_ID_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'KEY_NOT_UNIQUE',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NEITHER_ID_NOR_KEY',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'BusinessErrorCode',
                readonly code: 'GLOBAL_TENANT_REQUIRED',
                readonly [key:string]: any
            }
        ),
        "saveCompositeBook": AllErrors & (
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NULL_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_TARGET_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_DISSOCIATE_TARGETS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_CREATE_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_GENERATED_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_KEY_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_NON_ID_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'KEY_NOT_UNIQUE',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NEITHER_ID_NOR_KEY',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'BusinessErrorCode',
                readonly code: 'GLOBAL_TENANT_REQUIRED',
                readonly [key:string]: any
            }
        )
    },
    "bookStoreService": {
        "saveBookStore": AllErrors & (
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NULL_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_TARGET_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_DISSOCIATE_TARGETS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_CREATE_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_GENERATED_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_KEY_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_NON_ID_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'KEY_NOT_UNIQUE',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NEITHER_ID_NOR_KEY',
                readonly [key:string]: any
            }
        )
    },
    "treeService": {
        "saveTree": AllErrors & (
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NULL_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_TARGET_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_DISSOCIATE_TARGETS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'CANNOT_CREATE_TARGET',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_ID_GENERATOR',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_GENERATED_ID',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_KEY_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_NON_ID_PROPS',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NO_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'ILLEGAL_VERSION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'KEY_NOT_UNIQUE',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'SaveErrorCode',
                readonly code: 'NEITHER_ID_NOR_KEY',
                readonly [key:string]: any
            }
        )
    }
};
