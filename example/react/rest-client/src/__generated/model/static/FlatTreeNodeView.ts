export interface FlatTreeNodeView {
    readonly id: number;
    readonly name: string;
    readonly parentId?: number | undefined;
    readonly parentName?: string | undefined;
    readonly grandParentId?: number | undefined;
    readonly grandParentName?: string | undefined;
}
