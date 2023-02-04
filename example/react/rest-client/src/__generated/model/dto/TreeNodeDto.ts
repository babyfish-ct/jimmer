export type TreeNodeDto = {
    'DEFAULT': {
        readonly createdTime: string, 
        readonly modifiedTime: string, 
        readonly id: number, 
        readonly name: string, 
        readonly parent?: {readonly id: number}
    }, 
    'TreeService/RECURSIVE_FETCHER': {
        readonly id: number, 
        readonly createdTime: string, 
        readonly modifiedTime: string, 
        readonly name: string, 
        readonly childNodes: ReadonlyArray<{
            readonly id: number, 
            readonly createdTime: string, 
            readonly modifiedTime: string, 
            readonly name: string
        }>
    }
}