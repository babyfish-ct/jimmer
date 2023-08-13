export interface RecursiveTreeInput {
    
    readonly childNodes?: ReadonlyArray<RecursiveTreeInput_TargetOf_childNodes>;
    
    readonly name: string;
}

export interface RecursiveTreeInput_TargetOf_childNodes {
    
    readonly childNodes?: ReadonlyArray<RecursiveTreeInput_TargetOf_childNodes>;
    
    readonly name: string;
}
