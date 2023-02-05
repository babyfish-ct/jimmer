export type TreeNodeDto = {
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