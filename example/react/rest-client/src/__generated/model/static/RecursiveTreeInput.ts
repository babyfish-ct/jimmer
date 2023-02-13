export interface RecursiveTreeInput {
    
    readonly childNodes: ReadonlyArray<RecursiveTreeInput>;
    
    readonly name: string;
}
