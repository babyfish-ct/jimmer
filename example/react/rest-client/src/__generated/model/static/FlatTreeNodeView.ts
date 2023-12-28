export interface FlatTreeNodeView {
    readonly id: number;
    readonly name: string;
    readonly parentId: number | null | undefined;
    readonly parentName: string | null | undefined;
    readonly grandParentId: number | null | undefined;
    readonly grandParentName: string | null | undefined;
}
