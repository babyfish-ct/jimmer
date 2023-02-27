export type AllErrors = 
    {
        readonly family: "BusinessErrorCode",
        readonly code: "GLOBAL_TENANT_REQUIRED"
    }
;

export type ApiErrors = {
    "authorService": {
    },
    "bookService": {
        "saveBook": AllErrors & (
            {
                readonly family: 'BusinessErrorCode',
                readonly code: 'GLOBAL_TENANT_REQUIRED',
                readonly [key:string]: any
            }
        ),
        "saveCompositeBook": AllErrors & (
            {
                readonly family: 'BusinessErrorCode',
                readonly code: 'GLOBAL_TENANT_REQUIRED',
                readonly [key:string]: any
            }
        )
    },
    "bookStoreService": {
    },
    "treeService": {
    }
};
