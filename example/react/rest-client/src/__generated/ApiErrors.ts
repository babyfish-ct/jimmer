export type AllErrors = 
    {
        readonly family: "BusinessErrorCode",
        readonly code: "GLOBAL_TENANT_REQUIRED"
    } | 
    {
        readonly family: "BusinessErrorCode",
        readonly code: "ILLEGAL_BOOK_EDITION",
        readonly "min": number,
        readonly "max": number
    } | 
    {
        readonly family: "BusinessErrorCode",
        readonly code: "ILLEGAL_BOOK_PRICE",
        readonly "min": number,
        readonly "max": number
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
            } | 
            {
                readonly family: 'BusinessErrorCode',
                readonly code: 'ILLEGAL_BOOK_EDITION',
                readonly [key:string]: any
            } | 
            {
                readonly family: 'BusinessErrorCode',
                readonly code: 'ILLEGAL_BOOK_PRICE',
                readonly [key:string]: any
            }
        )
    },
    "bookStoreService": {
    },
    "treeService": {
    }
};
