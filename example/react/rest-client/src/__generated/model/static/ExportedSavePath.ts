export interface ExportedSavePath {
    
    readonly nodes: ReadonlyArray<ExportedSavePath_Node>;
    
    readonly rootTypeName: string;
}

export interface ExportedSavePath_Node {
    
    readonly prop: string;
    
    readonly targetTypeName: string;
}
