export interface TreeNode {
    
    readonly createdTime: string;
    
    readonly modifiedTime: string;
    
    readonly id: number;
    
    readonly name: string;
    
    readonly parent?: TreeNode;
    
    readonly childNodes: ReadonlyArray<TreeNode>;
}
