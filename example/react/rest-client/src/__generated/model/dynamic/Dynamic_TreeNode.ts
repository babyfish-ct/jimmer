export interface Dynamic_TreeNode {
    readonly id?: number;
    readonly name?: string;
    readonly parent?: Dynamic_TreeNode | undefined;
    readonly childNodes?: ReadonlyArray<Dynamic_TreeNode>;
}
