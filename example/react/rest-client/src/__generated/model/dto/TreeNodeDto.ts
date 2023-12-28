export type TreeNodeDto = {
    /**
     */
    'TreeService/RECURSIVE_FETCHER': {
        readonly id: number;
        readonly createdTime: string;
        readonly modifiedTime: string;
        readonly name: string;
        readonly childNodes?: ReadonlyArray<RecursiveType_1> | null | undefined;
    }
}
interface RecursiveType_1 {
    readonly id: number;
    readonly createdTime: string;
    readonly modifiedTime: string;
    readonly name: string;
    readonly childNodes?: ReadonlyArray<RecursiveType_1> | null | undefined;
}
